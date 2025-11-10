
package com.batodev.jigsawpuzzle

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.batodev.jigsawpuzzle.cut.PuzzleCurvesGenerator
import com.caverock.androidsvg.SVG
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// This annotation tells JUnit to run the test with AndroidJUnit4, which is a test runner for Android instrumented tests.
@RunWith(AndroidJUnit4::class)
class ImageMaskTest {

    // A lateinit property to hold the application context. It will be initialized in the setup method.
    private lateinit var context: Context

    // This method is annotated with @Before, so it will be executed before each test method.
    @Before
    fun setup() {
        // Get the context of the application under test.
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    @Throws(IOException::class)
    fun cutOutAllPuzzlePieces() {
        val overallStartTime = System.currentTimeMillis()

        // 1. Load the source image from assets to get its dimensions
        val assetManager = context.assets
        val inputStream = assetManager.open("img/00000-1215728026.jpg")
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        val puzzleWidth = originalBitmap.width
        val puzzleHeight = originalBitmap.height

        // 2. Generate the SVG string for the puzzle grid using image dimensions
        val piecesX = 3.0
        val piecesY = 4.0
        val totalPieces = (piecesX * piecesY).toInt()
        val generator = PuzzleCurvesGenerator().apply {
            width = puzzleWidth.toDouble()
            height = puzzleHeight.toDouble()
            xn = piecesX
            yn = piecesY
        }
        val svgString = generator.generateSvg()

        // 3. Render the SVG to a base bitmap
        val svg = SVG.getFromString(svgString)
        val baseMaskBitmap = Bitmap.createBitmap(puzzleWidth, puzzleHeight, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(baseMaskBitmap)
        svg.renderToCanvas(maskCanvas)

        // Get the pixels of the base mask once
        val basePixels = IntArray(puzzleWidth * puzzleHeight)
        baseMaskBitmap.getPixels(basePixels, 0, puzzleWidth, 0, 0, puzzleWidth, puzzleHeight)

        // 4. Use a thread pool to process all pieces in parallel
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        val outputDir = context.filesDir

        for (i in 0 until totalPieces) {
            executor.submit {
                val pieceX = i % piecesX.toInt()
                val pieceY = i / piecesX.toInt()

                // Work on a copy of the pixels to ensure thread safety
                val pixelsForPiece = basePixels.clone()

                // Find the starting point for flood fill in the center of the target piece
                val startX = (puzzleWidth / piecesX * (pieceX + 0.5)).toInt()
                val startY = (puzzleHeight / piecesY * (pieceY + 0.5)).toInt()

                // Isolate the piece with Flood Fill
                floodFill(pixelsForPiece, puzzleWidth, puzzleHeight, startX, startY, Color.TRANSPARENT, Color.WHITE)

                // Invert the mask: make the filled area the only opaque part
                for (j in pixelsForPiece.indices) {
                    if (pixelsForPiece[j] != Color.WHITE) {
                        pixelsForPiece[j] = Color.TRANSPARENT
                    }
                }

                // Create a bitmap for the isolated piece mask
                val pieceMaskBitmap = Bitmap.createBitmap(puzzleWidth, puzzleHeight, Bitmap.Config.ARGB_8888)
                pieceMaskBitmap.setPixels(pixelsForPiece, 0, puzzleWidth, 0, 0, puzzleWidth, puzzleHeight)

                // Apply the mask to the source image
                val maskedBitmap = Bitmap.createBitmap(puzzleWidth, puzzleHeight, Bitmap.Config.ARGB_8888)
                val pieceCanvas = Canvas(maskedBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                pieceCanvas.drawBitmap(pieceMaskBitmap, 0f, 0f, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                pieceCanvas.drawBitmap(originalBitmap, 0f, 0f, paint)

                // Crop the result to the visible puzzle piece
                val bounds = findVisibleBounds(maskedBitmap)
                val finalBitmap = if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                    Bitmap.createBitmap(maskedBitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
                } else {
                    maskedBitmap // Fallback
                }

                // Save the final piece
                val outputFile = File(outputDir, "puzzle_piece_$i.png")
                FileOutputStream(outputFile).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Log.d("ImageMaskTest", "Saved piece $i to ${outputFile.absolutePath}")
            }
        }

        // 5. Wait for all threads to finish and verify
        executor.shutdown()
        try {
            // Wait a reasonable amount of time for all pieces to be processed
            executor.awaitTermination(5, TimeUnit.MINUTES)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return // Exit if the wait is interrupted
        }

        val overallDuration = System.currentTimeMillis() - overallStartTime
        Log.d("ImageMaskTest", "--- Performance (Parallel) --- ")
        Log.d("ImageMaskTest", "Overall operation for $totalPieces pieces took: $overallDuration ms")

        // Verify all files were created
        for (i in 0 until totalPieces) {
            val outputFile = File(outputDir, "puzzle_piece_$i.png")
            assertTrue("Puzzle piece file #$i was not created.", outputFile.exists())
            assertTrue("Puzzle piece file #$i is empty.", outputFile.length() > 0)
        }
    }

    /**
     * Finds the bounding box of non-transparent pixels in a bitmap.
     */
    private fun findVisibleBounds(bitmap: Bitmap): Rect? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (pixels[index] ushr 24 != 0) { // Check alpha channel
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }

        return if (maxX < minX || maxY < minY) null else Rect(minX, minY, maxX + 1, maxY + 1)
    }

    /**
     * Fills a connected area using an iterative, queue-based approach on a pixel array.
     */
    private fun floodFill(pixels: IntArray, width: Int, height: Int, x: Int, y: Int, targetColor: Int, newColor: Int) {
        if (targetColor == newColor) return

        val queue: Queue<Point> = LinkedList()
        queue.add(Point(x, y))

        while (queue.isNotEmpty()) {
            val p = queue.poll() ?: continue

            if (p.x < 0 || p.x >= width || p.y < 0 || p.y >= height) continue

            val index = p.y * width + p.x
            if (pixels[index] == targetColor) {
                pixels[index] = newColor
                queue.add(Point(p.x + 1, p.y))
                queue.add(Point(p.x - 1, p.y))
                queue.add(Point(p.x, p.y + 1))
                queue.add(Point(p.x, p.y - 1))
            }
        }
    }
}
