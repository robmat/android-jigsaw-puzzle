package com.batodev.jigsawpuzzle.cut

import android.graphics.*
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

interface PuzzleCutter {
    @Throws(SVGParseException::class)
    fun cut(sourceImage: Bitmap, rows: Int, cols: Int, svgString: String?, imageView: ImageView, puzzleProgressListener: PuzzleProgressListener, pieces: List<PuzzlePiece>): List<Bitmap>
    companion object { fun default(): PuzzleCutter = MaskBitmapPuzzleCutter() }
}

class FloodFillPuzzleCutter : PuzzleCutter {
    private val numProcessors = Runtime.getRuntime().availableProcessors()
    override fun cut(sourceImage: Bitmap, rows: Int, cols: Int, svgString: String?, imageView: ImageView, puzzleProgressListener: PuzzleProgressListener, pieces: List<PuzzlePiece>): List<Bitmap> {
        val result: MutableList<Bitmap> = ArrayList()
        val svg = SVG.getFromString(svgString)
        val width = sourceImage.width
        val height = sourceImage.height
        val puzzleGridBitmap = createBitmap(width, height)
        val puzzleGridCanvas = Canvas(puzzleGridBitmap)
        val whiteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }
        puzzleGridCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), whiteFill)
        svg.renderToCanvas(puzzleGridCanvas)
        val executor = Executors.newFixedThreadPool(numProcessors)
        val progressCounter = AtomicInteger(0)
        val puzzlesCenterPoints = divideImage(puzzleGridBitmap, rows, cols)
        var puzzleIndex = 0
        for (rowIndex in 0 until rows) {
            for (colIndex in 0 until cols) {
                val piece = pieces[puzzleIndex++]
                executor.submit {
                    val puzzleCenter = puzzlesCenterPoints[rowIndex][colIndex]
                    val reg = floodFill(puzzleGridBitmap, puzzleCenter!!.x, puzzleCenter.y)
                    val regionWidth = reg.width
                    val regionHeight = reg.height
                    val regionMinX = reg.minX
                    val regionMinY = reg.minY
                    val puzzleBitmap = createBitmap(regionWidth + 1, regionHeight + 1)
                    reg.points.forEach(Consumer { (x1, y1): Point ->
                        val rgbSource = sourceImage[x1, y1]
                        puzzleBitmap[x1 - regionMinX, y1 - regionMinY] = rgbSource
                    })
                    synchronized(result) { result.add(puzzleBitmap) }
                    puzzleProgressListener.postToHandler {
                        piece.setImageBitmap(puzzleBitmap)
                        piece.pieceWidth = regionWidth
                        piece.pieceHeight = regionHeight
                        piece.xCoord = regionMinX + imageView.left
                        piece.yCoord = regionMinY + imageView.top
                    }
                    val progress = progressCounter.incrementAndGet()
                    puzzleProgressListener.postToHandler { puzzleProgressListener.onProgressUpdate(progress, rows * cols) }
                }
            }
        }
        executor.shutdown()
        Thread {
            try { executor.awaitTermination(1, TimeUnit.HOURS) } catch (e: InterruptedException) {
                FirebaseHelper.logException(imageView.context, "FloodFillPuzzleCutter.cut", e.message); throw RuntimeException(e)
            }
            puzzleProgressListener.postToHandler { puzzleProgressListener.onCuttingFinished() }
        }.start()
        return result
    }
    private fun floodFill(image: Bitmap, startX: Int, startY: Int): Region {
        val reg = Region(ArrayList())
        val queue: Queue<Point> = ArrayDeque()
        val width = image.width; val height = image.height
        if (startX !in 0 until width || startY !in 0 until height) return reg
        if (image[startX, startY] != Color.WHITE) return reg
        queue.add(Point(startX, startY))
        while (queue.isNotEmpty()) {
            val point = queue.poll()!!; val x = point.x; val y = point.y
            if (image[x, y] != Color.WHITE) continue
            image[x, y] = Color.GREEN; reg.points.add(Point(x, y))
            if (x > 0) queue.add(Point(x - 1, y)); if (x < width - 1) queue.add(Point(x + 1, y))
            if (y > 0) queue.add(Point(x, y - 1)); if (y < height - 1) queue.add(Point(x, y + 1))
        }
        return reg
    }
    private fun divideImage(image: Bitmap, rows: Int, cols: Int): Array<Array<Point?>> {
        val cellWidth = image.width / cols; val cellHeight = image.height / rows
        return Array(rows) { i -> Array(cols) { j -> Point(j * cellWidth + cellWidth / 2, i * cellHeight + cellHeight / 2) } }
    }
    internal class Point(var x: Int, var y: Int) { operator fun component1(): Int = x; operator fun component2(): Int = y }
    internal class Region(val points: MutableCollection<Point>) {
        private val maxX: Int get() = points.maxOfOrNull { it.x } ?: 0
        val minX: Int get() = points.minOfOrNull { it.x } ?: 0
        private val maxY: Int get() = points.maxOfOrNull { it.y } ?: 0
        val minY: Int get() = points.minOfOrNull { it.y } ?: 0
        val width: Int get() = maxX - minX; val height: Int get() = maxY - minY }
}

class MaskBitmapPuzzleCutter : PuzzleCutter {
    private val numProcessors = Runtime.getRuntime().availableProcessors()
    override fun cut(sourceImage: Bitmap, rows: Int, cols: Int, svgString: String?, imageView: ImageView, puzzleProgressListener: PuzzleProgressListener, pieces: List<PuzzlePiece>): List<Bitmap> {
        val width = sourceImage.width; val height = sourceImage.height
        val svg = SVG.getFromString(svgString)
        val baseMaskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(baseMaskBitmap); svg.renderToCanvas(maskCanvas)
        val basePixels = IntArray(width * height); baseMaskBitmap.getPixels(basePixels, 0, width, 0, 0, width, height)
        val executor = Executors.newFixedThreadPool(numProcessors)
        val progressCounter = AtomicInteger(0); val result = mutableListOf<Bitmap>()
        val totalPieces = rows * cols
        for (i in 0 until totalPieces) {
            val piece = pieces[i]
            executor.submit {
                val pieceX = i % cols; val pieceY = i / cols
                val pixelsForPiece = basePixels.clone()
                val startX = (width / cols.toDouble() * (pieceX + 0.5)).toInt()
                val startY = (height / rows.toDouble() * (pieceY + 0.5)).toInt()
                // The mask uses transparent as the target color and white as a temporary marker.
                // Use the simplified floodFill which uses fixed colors internally.
                floodFill(pixelsForPiece, width, height, startX, startY)
                for (j in pixelsForPiece.indices) if (pixelsForPiece[j] != Color.WHITE) pixelsForPiece[j] = Color.TRANSPARENT
                val pieceMaskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                pieceMaskBitmap.setPixels(pixelsForPiece, 0, width, 0, 0, width, height)
                val maskedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val pieceCanvas = Canvas(maskedBitmap); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                pieceCanvas.drawBitmap(pieceMaskBitmap, 0f, 0f, paint); paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                pieceCanvas.drawBitmap(sourceImage, 0f, 0f, paint)
                val bounds = findVisibleBounds(maskedBitmap)
                val finalBitmap = if (bounds != null && bounds.width() > 0 && bounds.height() > 0) Bitmap.createBitmap(maskedBitmap, bounds.left, bounds.top, bounds.width(), bounds.height()) else maskedBitmap
                synchronized(result) { result.add(finalBitmap) }
                puzzleProgressListener.postToHandler {
                    piece.setImageBitmap(finalBitmap)
                    piece.pieceWidth = finalBitmap.width
                    piece.pieceHeight = finalBitmap.height
                    if (bounds != null) { piece.xCoord = bounds.left + imageView.left; piece.yCoord = bounds.top + imageView.top }
                }
                val progress = progressCounter.incrementAndGet()
                puzzleProgressListener.postToHandler { puzzleProgressListener.onProgressUpdate(progress, totalPieces) }
            }
        }
        executor.shutdown()
        Thread {
            try { executor.awaitTermination(1, TimeUnit.HOURS) } catch (e: InterruptedException) {
                FirebaseHelper.logException(imageView.context, "MaskBitmapPuzzleCutter.cut", e.message); throw RuntimeException(e) }
            puzzleProgressListener.postToHandler { puzzleProgressListener.onCuttingFinished() }
        }.start()
        return result
    }
    private fun findVisibleBounds(bitmap: Bitmap): Rect? {
        val w = bitmap.width; val h = bitmap.height; val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w; var minY = h; var maxX = -1; var maxY = -1
        for (y in 0 until h) for (x in 0 until w) { val index = y * w + x; if (pixels[index] ushr 24 != 0) { minX = min(minX, x); minY = min(minY, y); maxX = max(maxX, x); maxY = max(maxY, y) } }
        return if (maxX < minX || maxY < minY) null else Rect(minX, minY, maxX + 1, maxY + 1)
    }
    // Flood fill that treats transparent pixels as the target and marks them with white. This
    // variant uses fixed colors to avoid redundant parameters and analyzer warnings.
    private fun floodFill(pixels: IntArray, width: Int, height: Int, x: Int, y: Int) {
        // We'll treat 'transparent' as target: alpha == 0. Mark visited pixels with opaque white.
        // Validate start point
        if (x !in 0..<width || y < 0 || y >= height) return
        val startIndex = y * width + x
        if ((pixels[startIndex] ushr 24) != 0) return // start pixel is not transparent
        val queue: Queue<Point> = ArrayDeque()
        queue.add(Point(x, y))
        while (queue.isNotEmpty()) {
            val p = queue.poll() ?: continue
            if (p.x !in 0..<width || p.y < 0 || p.y >= height) continue
            val index = p.y * width + p.x
            val pixel = pixels[index]
            // Alpha == 0 indicates transparent pixel
            if (pixel ushr 24 == 0) {
                pixels[index] = Color.WHITE
                queue.add(Point(p.x + 1, p.y)); queue.add(Point(p.x - 1, p.y)); queue.add(Point(p.x, p.y + 1)); queue.add(Point(p.x, p.y - 1))
            }
        }
    }
    private data class Point(val x: Int, val y: Int)
}
