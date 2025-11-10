
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

    // This annotation marks this function as a test case.
    @Test
    // This annotation declares that the test method can throw an IOException.
    @Throws(IOException::class)
    fun applyCircularMask() {
        // SECTION 1: Load the image from the assets folder.
        // Get the AssetManager, which provides access to application's raw asset files.
        val assetManager = context.assets
        // Open an input stream for the specified image file in the "img" subfolder of the assets.
        val inputStream = assetManager.open("img/00000-1215728026.jpg")
        // Decode the input stream into a Bitmap object.
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        // Record the start time of the masking operation to measure its duration.
        val startTime = System.currentTimeMillis()

        // SECTION 2: Create a circular mask and apply it to the image.
        // Create a new, mutable bitmap with the same dimensions as the original, using the ARGB_8888 config for high quality.
        val outputBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        // Create a Canvas to draw on the new bitmap.
        val canvas = Canvas(outputBitmap)
        // Initialize a Paint object for drawing.
        val paint = Paint()
        // Create a Rect that represents the full dimensions of the bitmap.
        val rect = Rect(0, 0, originalBitmap.width, originalBitmap.height)
        // Create a RectF (a rectangle with float coordinates) from the integer Rect. This is needed for drawing rounded rectangles.
        val rectF = RectF(rect)
        // Calculate the radius for the circular corners. Half of the width will create a circle.
        val roundPx = (originalBitmap.width / 2).toFloat()

        // Enable anti-aliasing on the paint to smooth out the edges of the circle.
        paint.isAntiAlias = true
        // Clear the canvas with a transparent color.
        canvas.drawARGB(0, 0, 0, 0)
        // Draw a rounded rectangle (which will be a circle) onto the canvas. This will act as the mask shape.
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)

        // Set the PorterDuff Xfermode to SRC_IN. This mode means "draw the source image, but only where it overlaps with the destination (the circle)".
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        // Draw the original bitmap onto the canvas, applying the mask.
        canvas.drawBitmap(originalBitmap, rect, rect, paint)

        // Record the end time of the masking operation.
        val endTime = System.currentTimeMillis()
        // Log the duration of the masking operation to Android's Logcat.
        Log.d("ImageMaskTest", "Image masking took ${endTime - startTime} ms")

        // SECTION 3: Save the resulting masked image to a file.
        // Get the application's internal file storage directory.
        val cacheDir = context.filesDir
        // Create a File object for the output image.
        val outputFile = File(cacheDir, "masked_image.png")
        // Open a FileOutputStream for the output file. The 'use' block ensures the stream is closed automatically.
        FileOutputStream(outputFile).use { out ->
            // Compress the output bitmap to a PNG file format with 100% quality and write it to the output stream.
            outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // SECTION 4: Verify that the file was created successfully.
        // Assert that the output file exists. If it doesn't, the test will fail with the provided message.
        assertTrue("Masked image file was not created.", outputFile.exists())
        // Assert that the output file is not empty. If it is, the test will fail with the provided message.
        assertTrue("Masked image file is empty.", outputFile.length() > 0)
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
        val piecesX = 4.0
        val piecesY = 3.0
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

        // 6. Save the final cut-out puzzle piece
        val cacheDir = context.cacheDir
        val outputFile = File(cacheDir, "puzzle_piece.png")
        FileOutputStream(outputFile).use { out ->
            resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
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
