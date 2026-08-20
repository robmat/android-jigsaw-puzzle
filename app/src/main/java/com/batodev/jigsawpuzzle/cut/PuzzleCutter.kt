package com.batodev.jigsawpuzzle.cut

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.batodev.jigsawpuzzle.helpers.FirebaseHelper
import com.batodev.jigsawpuzzle.logic.PuzzleProgressListener
import com.batodev.jigsawpuzzle.view.PuzzlePiece
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

private const val AWAIT_TERMINATION_HOURS = 1L

/**
 * The parameters required to cut a source image into puzzle pieces.
 * @param sourceImage The original image to be cut.
 * @param rows The number of rows for the puzzle grid.
 * @param cols The number of columns for the puzzle grid.
 * @param svgString The SVG string defining the puzzle piece shapes.
 * @param imageView The ImageView where the puzzle pieces will be displayed.
 * @param puzzleProgressListener A listener to report progress updates and completion.
 * @param pieces A list of PuzzlePiece objects to populate with the cut bitmaps.
 */
data class PuzzleCutRequest(
    val sourceImage: Bitmap,
    val rows: Int,
    val cols: Int,
    val svgString: String?,
    val imageView: ImageView,
    val puzzleProgressListener: PuzzleProgressListener,
    val pieces: List<PuzzlePiece>,
)

interface PuzzleCutter {
    @Throws(SVGParseException::class)
    fun cut(request: PuzzleCutRequest): List<Bitmap>

    companion object {
        fun default(): PuzzleCutter = MaskBitmapPuzzleCutter()
    }
}

private fun awaitCompletionAsync(
    executor: ExecutorService,
    logTag: String,
    imageView: ImageView,
    puzzleProgressListener: PuzzleProgressListener,
) {
    Thread {
        try {
            executor.awaitTermination(AWAIT_TERMINATION_HOURS, TimeUnit.HOURS)
        } catch (e: InterruptedException) {
            FirebaseHelper.logException(imageView.context, logTag, e.message)
            Thread.currentThread().interrupt()
        }
        puzzleProgressListener.postToHandler { puzzleProgressListener.onCuttingFinished() }
    }.start()
}

/** The ImageView and progress listener a [PuzzleCutter] reports piece placement and progress to. */
private class PieceOutput(
    val imageView: ImageView,
    val puzzleProgressListener: PuzzleProgressListener,
)

/** Shared mutable state tracking cut pieces as they complete across the cutting thread pool. */
private class ProgressTracking(
    val result: MutableList<Bitmap>,
    val totalPieces: Int,
    val progressCounter: AtomicInteger,
)

class FloodFillPuzzleCutter : PuzzleCutter {
    private val numProcessors = Runtime.getRuntime().availableProcessors()

    override fun cut(request: PuzzleCutRequest): List<Bitmap> {
        val result: MutableList<Bitmap> = ArrayList()
        val svg = SVG.getFromString(request.svgString)
        val width = request.sourceImage.width
        val height = request.sourceImage.height
        val puzzleGridBitmap = createBitmap(width, height)
        val puzzleGridCanvas = Canvas(puzzleGridBitmap)
        val whiteFill =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.WHITE
            }
        puzzleGridCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), whiteFill)
        svg.renderToCanvas(puzzleGridCanvas)
        val executor = Executors.newFixedThreadPool(numProcessors)
        val context =
            CuttingContext(
                sourceImage = request.sourceImage,
                puzzleGridBitmap = puzzleGridBitmap,
                output = PieceOutput(request.imageView, request.puzzleProgressListener),
                tracking = ProgressTracking(result, request.rows * request.cols, AtomicInteger(0)),
            )
        val puzzlesCenterPoints = divideImage(puzzleGridBitmap, request.rows, request.cols)
        var puzzleIndex = 0
        for (rowIndex in 0 until request.rows) {
            for (colIndex in 0 until request.cols) {
                val piece = request.pieces[puzzleIndex++]
                val puzzleCenter = puzzlesCenterPoints[rowIndex][colIndex]!!
                executor.submit(cutPieceJob(context, puzzleCenter, piece))
            }
        }
        executor.shutdown()
        awaitCompletionAsync(executor, "FloodFillPuzzleCutter.cut", request.imageView, request.puzzleProgressListener)
        return result
    }

    private class CuttingContext(
        val sourceImage: Bitmap,
        val puzzleGridBitmap: Bitmap,
        val output: PieceOutput,
        val tracking: ProgressTracking,
    )

    private fun cutPieceJob(
        context: CuttingContext,
        puzzleCenter: Point,
        piece: PuzzlePiece,
    ): Runnable =
        Runnable {
            val reg = floodFill(context.puzzleGridBitmap, puzzleCenter.x, puzzleCenter.y)
            val regionWidth = reg.width
            val regionHeight = reg.height
            val regionMinX = reg.minX
            val regionMinY = reg.minY
            val puzzleBitmap = createBitmap(regionWidth + 1, regionHeight + 1)
            reg.points.forEach(
                Consumer { (x1, y1): Point ->
                    val rgbSource = context.sourceImage[x1, y1]
                    puzzleBitmap[x1 - regionMinX, y1 - regionMinY] = rgbSource
                },
            )
            synchronized(context.tracking.result) { context.tracking.result.add(puzzleBitmap) }
            context.output.puzzleProgressListener.postToHandler {
                piece.setImageBitmap(puzzleBitmap)
                piece.pieceWidth = regionWidth
                piece.pieceHeight = regionHeight
                piece.xCoord = regionMinX + context.output.imageView.left
                piece.yCoord = regionMinY + context.output.imageView.top
            }
            val progress = context.tracking.progressCounter.incrementAndGet()
            context.output.puzzleProgressListener.postToHandler {
                context.output.puzzleProgressListener.onProgressUpdate(progress, context.tracking.totalPieces)
            }
        }

    private fun floodFill(
        image: Bitmap,
        startX: Int,
        startY: Int,
    ): Region {
        val reg = Region(ArrayList())
        val queue: Queue<Point> = ArrayDeque()
        val width = image.width
        val height = image.height
        if (isOutOfBounds(startX, startY, width, height) || image[startX, startY] != Color.WHITE) {
            return reg
        }
        queue.add(Point(startX, startY))
        while (queue.isNotEmpty()) {
            val point = queue.poll()!!
            val x = point.x
            val y = point.y
            if (image[x, y] != Color.WHITE) continue
            image[x, y] = Color.GREEN
            reg.points.add(Point(x, y))
            if (x > 0) queue.add(Point(x - 1, y))
            if (x < width - 1) queue.add(Point(x + 1, y))
            if (y > 0) queue.add(Point(x, y - 1))
            if (y < height - 1) queue.add(Point(x, y + 1))
        }
        return reg
    }

    private fun isOutOfBounds(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ): Boolean = x !in 0 until width || y !in 0 until height

    private fun divideImage(
        image: Bitmap,
        rows: Int,
        cols: Int,
    ): Array<Array<Point?>> {
        val cellWidth = image.width / cols
        val cellHeight = image.height / rows
        return Array(rows) { i ->
            Array(cols) { j -> Point(j * cellWidth + cellWidth / 2, i * cellHeight + cellHeight / 2) }
        }
    }

    internal class Point(
        var x: Int,
        var y: Int,
    ) {
        operator fun component1(): Int = x

        operator fun component2(): Int = y
    }

    internal class Region(
        val points: MutableCollection<Point>,
    ) {
        private val maxX: Int get() = points.maxOfOrNull { it.x } ?: 0
        val minX: Int get() = points.minOfOrNull { it.x } ?: 0
        private val maxY: Int get() = points.maxOfOrNull { it.y } ?: 0
        val minY: Int get() = points.minOfOrNull { it.y } ?: 0
        val width: Int get() = maxX - minX
        val height: Int get() = maxY - minY
    }
}

class MaskBitmapPuzzleCutter : PuzzleCutter {
    companion object {
        private const val PIECE_CENTER_OFFSET = 0.5
        private const val ALPHA_SHIFT_BITS = 24
    }

    private val numProcessors = Runtime.getRuntime().availableProcessors()

    override fun cut(request: PuzzleCutRequest): List<Bitmap> {
        val width = request.sourceImage.width
        val height = request.sourceImage.height
        val svg = SVG.getFromString(request.svgString)
        val baseMaskBitmap = createBitmap(width, height)
        val maskCanvas = Canvas(baseMaskBitmap)
        svg.renderToCanvas(maskCanvas)
        val basePixels = IntArray(width * height)
        baseMaskBitmap.getPixels(basePixels, 0, width, 0, 0, width, height)
        val executor = Executors.newFixedThreadPool(numProcessors)
        val result = mutableListOf<Bitmap>()
        val totalPieces = request.rows * request.cols
        val context =
            CuttingContext(
                sourceImage = request.sourceImage,
                basePixels = basePixels,
                grid = PieceGrid(width, height, request.rows, request.cols),
                output = PieceOutput(request.imageView, request.puzzleProgressListener),
                tracking = ProgressTracking(result, totalPieces, AtomicInteger(0)),
            )
        for (i in 0 until totalPieces) {
            executor.submit(cutPieceJob(context, i, request.pieces[i]))
        }
        executor.shutdown()
        awaitCompletionAsync(executor, "MaskBitmapPuzzleCutter.cut", request.imageView, request.puzzleProgressListener)
        return result
    }

    private class PieceGrid(
        val width: Int,
        val height: Int,
        val rows: Int,
        val cols: Int,
    )

    private class CuttingContext(
        val sourceImage: Bitmap,
        val basePixels: IntArray,
        val grid: PieceGrid,
        val output: PieceOutput,
        val tracking: ProgressTracking,
    )

    private fun cutPieceJob(
        context: CuttingContext,
        pieceIndex: Int,
        piece: PuzzlePiece,
    ): Runnable =
        Runnable {
            val grid = context.grid
            val pieceX = pieceIndex % grid.cols
            val pieceY = pieceIndex / grid.cols
            val pixelsForPiece = context.basePixels.clone()
            val startX = (grid.width / grid.cols.toDouble() * (pieceX + PIECE_CENTER_OFFSET)).toInt()
            val startY = (grid.height / grid.rows.toDouble() * (pieceY + PIECE_CENTER_OFFSET)).toInt()
            // The mask uses transparent as the target color and white as a temporary marker.
            // Use the simplified floodFill which uses fixed colors internally.
            floodFill(pixelsForPiece, grid.width, grid.height, startX, startY)
            for (j in pixelsForPiece.indices) {
                if (pixelsForPiece[j] != Color.WHITE) pixelsForPiece[j] = Color.TRANSPARENT
            }
            val finalBitmap = buildMaskedBitmap(context.sourceImage, pixelsForPiece, grid.width, grid.height)
            synchronized(context.tracking.result) { context.tracking.result.add(finalBitmap.bitmap) }
            context.output.puzzleProgressListener.postToHandler {
                piece.setImageBitmap(finalBitmap.bitmap)
                piece.pieceWidth = finalBitmap.bitmap.width
                piece.pieceHeight = finalBitmap.bitmap.height
                if (finalBitmap.bounds != null) {
                    piece.xCoord = finalBitmap.bounds.left + context.output.imageView.left
                    piece.yCoord = finalBitmap.bounds.top + context.output.imageView.top
                }
            }
            val progress = context.tracking.progressCounter.incrementAndGet()
            context.output.puzzleProgressListener.postToHandler {
                context.output.puzzleProgressListener.onProgressUpdate(progress, context.tracking.totalPieces)
            }
        }

    private class MaskedPieceBitmap(
        val bitmap: Bitmap,
        val bounds: Rect?,
    )

    private fun buildMaskedBitmap(
        sourceImage: Bitmap,
        pixelsForPiece: IntArray,
        width: Int,
        height: Int,
    ): MaskedPieceBitmap {
        val pieceMaskBitmap = createBitmap(width, height)
        pieceMaskBitmap.setPixels(pixelsForPiece, 0, width, 0, 0, width, height)
        val maskedBitmap = createBitmap(width, height)
        val pieceCanvas = Canvas(maskedBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        pieceCanvas.drawBitmap(pieceMaskBitmap, 0f, 0f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        pieceCanvas.drawBitmap(sourceImage, 0f, 0f, paint)
        val bounds = findVisibleBounds(maskedBitmap)
        val finalBitmap =
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                Bitmap.createBitmap(maskedBitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
            } else {
                maskedBitmap
            }
        return MaskedPieceBitmap(finalBitmap, bounds)
    }

    private fun findVisibleBounds(bitmap: Bitmap): Rect? {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        for (y in 0 until h) {
            for (x in 0 until w) {
                val index = y * w + x
                if (pixels[index] ushr ALPHA_SHIFT_BITS != 0) {
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)
                }
            }
        }
        return if (maxX < minX || maxY < minY) null else Rect(minX, minY, maxX + 1, maxY + 1)
    }

    // Flood fill that treats transparent pixels as the target and marks them with white. This
    // variant uses fixed colors to avoid redundant parameters and analyzer warnings.
    private fun floodFill(
        pixels: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
    ) {
        // We'll treat 'transparent' as target: alpha == 0. Mark visited pixels with opaque white.
        if (isOutOfBounds(x, y, width, height)) return
        val startIndex = y * width + x
        if ((pixels[startIndex] ushr ALPHA_SHIFT_BITS) != 0) return // start pixel is not transparent
        val queue: Queue<Point> = ArrayDeque()
        queue.add(Point(x, y))
        while (queue.isNotEmpty()) {
            val p = queue.poll()!!
            if (isOutOfBounds(p.x, p.y, width, height)) continue
            val index = p.y * width + p.x
            val pixel = pixels[index]
            // Alpha == 0 indicates transparent pixel
            if (pixel ushr ALPHA_SHIFT_BITS == 0) {
                pixels[index] = Color.WHITE
                queue.add(Point(p.x + 1, p.y))
                queue.add(Point(p.x - 1, p.y))
                queue.add(Point(p.x, p.y + 1))
                queue.add(Point(p.x, p.y - 1))
            }
        }
    }

    private fun isOutOfBounds(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ): Boolean = x !in 0 until width || y !in 0 until height

    private data class Point(
        val x: Int,
        val y: Int,
    )
}
