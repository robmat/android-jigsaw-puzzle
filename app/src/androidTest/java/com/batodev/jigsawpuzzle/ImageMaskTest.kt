
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
    fun cutOutSinglePuzzlePiece() {
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
        val generator = PuzzleCurvesGenerator().apply {
            width = puzzleWidth.toDouble()
            height = puzzleHeight.toDouble()
            xn = piecesX
            yn = piecesY
        }
        val svgString = generator.generateSvg()

        // 3. Render the SVG and create a mask from it
        val maskingStartTime = System.currentTimeMillis()
        val svg = SVG.getFromString(svgString)
        val maskBitmap = Bitmap.createBitmap(puzzleWidth, puzzleHeight, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)
        svg.renderToCanvas(maskCanvas)

        // OPTIMIZATION: Get pixels into a buffer for fast manipulation
        val pixels = IntArray(puzzleWidth * puzzleHeight)
        maskBitmap.getPixels(pixels, 0, puzzleWidth, 0, 0, puzzleWidth, puzzleHeight)

        // 4. Use Flood Fill on the pixel buffer to isolate a single piece
        val startX = (puzzleWidth / piecesX / 2).toInt()
        val startY = (puzzleHeight / piecesY / 2).toInt()
        val floodFillStartTime = System.currentTimeMillis()
        floodFill(pixels, puzzleWidth, puzzleHeight, startX, startY, Color.TRANSPARENT, Color.WHITE)
        val floodFillDuration = System.currentTimeMillis() - floodFillStartTime

        // Invert the mask in the buffer
        for (i in pixels.indices) {
            if (pixels[i] != Color.WHITE) {
                pixels[i] = Color.TRANSPARENT
            }
        }

        // OPTIMIZATION: Write the modified pixels back to the bitmap
        maskBitmap.setPixels(pixels, 0, puzzleWidth, 0, 0, puzzleWidth, puzzleHeight)
        val maskingDuration = System.currentTimeMillis() - maskingStartTime

        // 5. Apply the mask to the source image
        val cuttingStartTime = System.currentTimeMillis()
        val resultBitmap = Bitmap.createBitmap(puzzleWidth, puzzleHeight, Bitmap.Config.ARGB_8888)
        val resultCanvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        resultCanvas.drawBitmap(maskBitmap, 0f, 0f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        resultCanvas.drawBitmap(originalBitmap, 0f, 0f, paint)
        val cuttingDuration = System.currentTimeMillis() - cuttingStartTime

        // 6. Crop the result to the visible puzzle piece and save it
        val bounds = findVisibleBounds(resultBitmap)
        val finalBitmap = if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
            Bitmap.createBitmap(resultBitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
        } else {
            resultBitmap // Fallback
        }

        val cacheDir = context.cacheDir
        val outputFile = File(cacheDir, "puzzle_piece.png")
        FileOutputStream(outputFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // 7. Log timings and verify the file was created
        val overallDuration = System.currentTimeMillis() - overallStartTime
        Log.d("ImageMaskTest", "--- Performance (Optimized) --- ")
        Log.d("ImageMaskTest", "Flood Fill took: $floodFillDuration ms")
        Log.d("ImageMaskTest", "Total Masking (Render + Flood Fill + Invert) took: $maskingDuration ms")
        Log.d("ImageMaskTest", "Cutting (Applying Mask) took: $cuttingDuration ms")
        Log.d("ImageMaskTest", "Overall operation took: $overallDuration ms")
        Log.d("ImageMaskTest", "Saved puzzle piece to ${outputFile.absolutePath}")
        assertTrue("Puzzle piece file was not created.", outputFile.exists())
        assertTrue("Puzzle piece file is empty.", outputFile.length() > 0)
    }

    /**
     * Finds the bounding box of non-transparent pixels in a bitmap.
     * @param bitmap The bitmap to scan.
     * @return A Rect containing the bounds of the visible pixels, or null if the bitmap is fully transparent.
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
                // Check if the pixel is not transparent (alpha channel is not 0)
                if (pixels[index] ushr 24 != 0) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }

        return if (maxX < minX || maxY < minY) {
            null // No visible pixels found
        } else {
            // Rect's right and bottom parameters are exclusive
            Rect(minX, minY, maxX + 1, maxY + 1)
        }
    }

    /**
     * Fills a connected area of a bitmap with a new color, starting from a seed point.
     * This optimized version operates on a 1D array of pixels for performance.
     * @param pixels The array of pixels to modify.
     * @param width The width of the source bitmap.
     * @param height The height of the source bitmap.
     * @param x The starting x-coordinate.
     * @param y The starting y-coordinate.
     * @param targetColor The color of the area to be filled.
     * @param newColor The color to fill the area with.
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
