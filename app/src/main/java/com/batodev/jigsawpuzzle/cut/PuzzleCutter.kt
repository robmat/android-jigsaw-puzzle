package com.batodev.jigsawpuzzle.cut

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.ImageView
import com.batodev.jigsawpuzzle.activity.PuzzleActivity
import com.batodev.jigsawpuzzle.view.PuzzlePiece
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import java.util.concurrent.atomic.AtomicInteger

object PuzzleCutter {
    private val numProcessors = Runtime.getRuntime().availableProcessors()
    @Throws(SVGParseException::class)
    fun cut(
        sourceImage: Bitmap,
        rows: Int,
        cols: Int,
        svgString: String?,
        imageView: ImageView,
        puzzleActivity: PuzzleActivity,
        pieces: List<PuzzlePiece>,
    ): List<Bitmap> {
        val result: MutableList<Bitmap> = ArrayList()
        val startTime = System.currentTimeMillis()
        val svg = SVG.getFromString(svgString)
        val width = sourceImage.width
        val height = sourceImage.height
        val puzzleGridBitmap = createBitmap(width, height)
        val puzzleGridCanvas = Canvas(puzzleGridBitmap)
        val whiteFill = Paint()
        whiteFill.style = Paint.Style.FILL
        whiteFill.color = Color.WHITE
        puzzleGridCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), whiteFill)
        svg.renderToCanvas(puzzleGridCanvas)
        val executor = Executors.newFixedThreadPool(numProcessors)
        val progressCounter = AtomicInteger(0)
        val puzzlesCenterPoints = divideImage(puzzleGridBitmap, rows, cols)
        var puzzleIndex = 0
        for (rowIndex in 0 until rows) {
            for (colIndex in 0 until cols) {
                val piece = pieces[puzzleIndex++]
                val puzzleCutJob = Runnable {
                    val puzzleCenter = puzzlesCenterPoints[rowIndex][colIndex]
                    val reg = floodFill(puzzleGridBitmap, puzzleCenter!!.x, puzzleCenter.y)
                    val regionWidth = reg.width
                    val regionHeight = reg.height
                    val regionMinX = reg.minX
                    val regionMinY = reg.minY
                    val puzzleBitmap = createBitmap(regionWidth + 1, regionHeight + 1)
                    println("Flood fill took: " + (System.currentTimeMillis() - startTime) + "ms")
                    reg.points.forEach(Consumer { (x1, y1): Point ->
                        val rgbSource = sourceImage[x1, y1]
                        val x = x1 - regionMinX
                        val y = y1 - regionMinY
                        puzzleBitmap[x, y] = rgbSource
                    })
                    result.add(puzzleBitmap)
                    val setPuzzleImageAndPositions = Runnable {
                        piece.setImageBitmap(puzzleBitmap)
                        piece.pieceWidth = regionWidth
                        piece.pieceHeight = regionHeight
                        piece.xCoord = regionMinX + imageView.left + 4
                        piece.yCoord = regionMinY + imageView.top + 7
                    }
                    puzzleActivity.postToHandler(setPuzzleImageAndPositions)
                    val progress = progressCounter.incrementAndGet()
                    puzzleActivity.postToHandler {
                        puzzleActivity.updateProgress(progress, rows * cols)
                    }
                }
                executor.submit(puzzleCutJob)
                println("Filling target took: " + (System.currentTimeMillis() - startTime) + "ms")
            }
        }
        executor.shutdown()
        Thread {
            try {
                val terminated = executor.awaitTermination(1, TimeUnit.HOURS)
                println(terminated)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            puzzleActivity.postToHandler { puzzleActivity.updateProgress(rows * cols, rows * cols) }
        }.start()
        return result
    }

    private fun floodFill(image: Bitmap, startX: Int, startY: Int): Region {
        val reg = Region(ArrayList())
        val queue: Queue<Point> = ArrayDeque()
        val width = image.width
        val height = image.height

        // Check if starting point is within image bounds
        if (startX < 0 || startY < 0 || startX >= width || startY >= height) {
            return reg
        }

        // Check if starting point color is same as target color
        if (image[startX, startY] != Color.WHITE) {
            return reg
        }

        // Add starting point to queue
        queue.add(Point(startX, startY))

        // Perform flood fill
        while (!queue.isEmpty()) {
            val point = queue.poll()!!
            val x = point.x
            val y = point.y

            // Check current pixel color
            if (image[x, y] != Color.WHITE) {
                continue
            }

            // Fill current pixel with fill color
            image[x, y] = Color.GREEN
            reg.points.add(Point(x, y))

            // Add neighboring pixels to queue
            if (x > 0) {
                queue.add(Point(x - 1, y))
            }
            if (x < width - 1) {
                queue.add(Point(x + 1, y))
            }
            if (y > 0) {
                queue.add(Point(x, y - 1))
            }
            if (y < height - 1) {
                queue.add(Point(x, y + 1))
            }
        }
        return reg
    }

    private fun divideImage(image: Bitmap, rows: Int, cols: Int): Array<Array<Point?>> {
        val width = image.width
        val height = image.height

        // Calculate the width and height of each cell
        val cellWidth = width / cols
        val cellHeight = height / rows
        val cellCenters = Array(rows) { arrayOfNulls<Point>(cols) }

        // Loop through each cell and find its center
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                // Calculate the coordinates of the cell center
                val centerX = j * cellWidth + cellWidth / 2
                val centerY = i * cellHeight + cellHeight / 2
                cellCenters[i][j] = Point(centerX, centerY)
            }
        }
        return cellCenters
    }

    internal class Point(var x: Int, var y: Int) {
        operator fun component1(): Int {
            return x
        }

        operator fun component2(): Int {
            return y
        }
    }
    internal class Region(val points: MutableCollection<Point>) {
        private val maxX: Int
            get() = points.stream().map(Point::x).max { obj: Int, anotherInteger: Int? ->
                obj.compareTo(
                    anotherInteger!!
                )
            }.orElse(0)
        val minX: Int
            get() = points.stream().map(Point::x).min { obj: Int, anotherInteger: Int? ->
                obj.compareTo(
                    anotherInteger!!
                )
            }.orElse(0)
        private val maxY: Int
            get() = points.stream().map(Point::y).max { obj: Int, anotherInteger: Int? ->
                obj.compareTo(
                    anotherInteger!!
                )
            }.orElse(0)
        val minY: Int
            get() = points.stream().map(Point::y).min { obj: Int, anotherInteger: Int? ->
                obj.compareTo(
                    anotherInteger!!
                )
            }.orElse(0)
        val width: Int
            get() = maxX - minX
        val height: Int
            get() = maxY - minY
    }
}
