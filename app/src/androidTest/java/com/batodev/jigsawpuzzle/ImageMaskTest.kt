package com.batodev.jigsawpuzzle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class ImageMaskTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    @Throws(IOException::class)
    fun cutOutAllPuzzlePieces() {
        // Desired puzzle grid (can adjust as needed)
        val rows = 6
        val cols = 4

        // Try to load provided sample asset image; fallback to synthetic gradient if missing
        val assetPath = "img/00000-1215728026.jpg"
        val originalBitmap: Bitmap =
            try {
                val assetManager = context.assets
                val inputStream: InputStream = assetManager.open(assetPath)
                BitmapFactory.decodeStream(inputStream).also { inputStream.close() }
            } catch (e: Exception) {
                Log.w("PuzzleCutterBenchmark", "Asset $assetPath not found, using synthetic gradient: ${e.message}")
                val w = 960
                val h = 720
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                    val c = Canvas(this)
                    val p = Paint()
                    for (y in 0 until h) {
                        val ratio =
                            y / h.toFloat()
                        p.color = Color.rgb((255 * ratio).toInt(), (255 * (1 - ratio)).toInt(), 128)
                        c.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), p)
                    }
                }
            }
        val puzzleWidth = originalBitmap.width
        val puzzleHeight = originalBitmap.height
        Log.i("PuzzleCutterBenchmark", "Loaded bitmap ${puzzleWidth}x$puzzleHeight")

        // Generate SVG for puzzle curves based on actual bitmap size
        val generator =
            com.batodev.jigsawpuzzle.cut.PuzzleCurvesGenerator().apply {
                this.width = puzzleWidth.toDouble()
                this.height = puzzleHeight.toDouble()
                this.xn = cols.toDouble()
                this.yn = rows.toDouble()
            }
        val svgString = generator.generateSvg()

        // Dummy ImageView (needed for cutter coordinate alignment)
        val imageView = android.widget.ImageView(context)

        // Helper to allocate PuzzlePiece placeholders
        fun createPieces(): MutableList<com.batodev.jigsawpuzzle.view.PuzzlePiece> {
            val pieces = mutableListOf<com.batodev.jigsawpuzzle.view.PuzzlePiece>()
            val pieceWidth = puzzleWidth / cols
            val pieceHeight = puzzleHeight / rows
            var yCoord = 0
            for (r in 0 until rows) {
                var xCoord = 0
                for (c in 0 until cols) {
                    val offsetX = if (c > 0) pieceWidth / 3 else 0
                    val offsetY = if (r > 0) pieceHeight / 3 else 0
                    val p =
                        com.batodev.jigsawpuzzle.view
                            .PuzzlePiece(context)
                    p.xCoord = xCoord - offsetX + imageView.left + 4
                    p.yCoord = yCoord - offsetY + imageView.top + 7
                    p.pieceWidth = pieceWidth + offsetX
                    p.pieceHeight = pieceHeight + offsetY
                    pieces.add(p)
                    xCoord += pieceWidth
                }
                yCoord += pieceHeight
            }
            return pieces
        }

        // Completion flags
        val floodFinished = AtomicBoolean(false)
        val maskFinished = AtomicBoolean(false)

        // Stub progress listener template factory
        fun progressListener(doneFlag: AtomicBoolean) =
            object : com.batodev.jigsawpuzzle.logic.PuzzleProgressListener {
                override fun onProgressUpdate(
                    progress: Int,
                    max: Int,
                ) { /* ignore granular */ }

                override fun onCuttingFinished() {
                    doneFlag.set(true)
                }

                override fun postToHandler(r: Runnable) {
                    r.run()
                }
            }

        val expectedPieces = rows * cols

        // FloodFill benchmark
        val floodFillCutter =
            com.batodev.jigsawpuzzle.cut
                .FloodFillPuzzleCutter()
        val floodPieces = createPieces()
        val floodResultBitmaps =
            floodFillCutter.cut(
                com.batodev.jigsawpuzzle.cut.PuzzleCutRequest(
                    originalBitmap,
                    rows,
                    cols,
                    svgString,
                    imageView,
                    progressListener(floodFinished),
                    floodPieces,
                ),
            )
        val floodStartNs = System.nanoTime()
        // Wait until finished or timeout
        val floodTimeoutNs = 30_000_000_000L // 30 seconds
        while (!floodFinished.get() && System.nanoTime() - floodStartNs < floodTimeoutNs) {
            Thread.sleep(25)
        }
        val floodDurationMs = (System.nanoTime() - floodStartNs) / 1_000_000.0

        // MaskBitmap benchmark
        val maskCutter =
            com.batodev.jigsawpuzzle.cut
                .MaskBitmapPuzzleCutter()
        val maskPieces = createPieces()
        val maskResultBitmaps =
            maskCutter.cut(
                com.batodev.jigsawpuzzle.cut.PuzzleCutRequest(
                    originalBitmap,
                    rows,
                    cols,
                    svgString,
                    imageView,
                    progressListener(maskFinished),
                    maskPieces,
                ),
            )
        val maskStartNs = System.nanoTime()
        val maskTimeoutNs = 30_000_000_000L
        while (!maskFinished.get() && System.nanoTime() - maskStartNs < maskTimeoutNs) {
            Thread.sleep(25)
        }
        val maskDurationMs = (System.nanoTime() - maskStartNs) / 1_000_000.0

        Log.i(
            "PuzzleCutterBenchmark",
            "FloodFill pieces=${floodResultBitmaps.size} elapsedMs=${"%.2f".format(
                floodDurationMs,
            )} finished=${floodFinished.get()}",
        )
        Log.i(
            "PuzzleCutterBenchmark",
            "MaskBitmap pieces=${maskResultBitmaps.size} elapsedMs=${"%.2f".format(
                maskDurationMs,
            )} finished=${maskFinished.get()}",
        )

        // Assertions
        assertTrue("FloodFill did not finish in time", floodFinished.get())
        assertTrue("MaskBitmap did not finish in time", maskFinished.get())
        assertTrue("FloodFill piece count mismatch", floodResultBitmaps.size == expectedPieces)
        assertTrue("MaskBitmap piece count mismatch", maskResultBitmaps.size == expectedPieces)
        assertTrue("FloodFill contains empty bitmap", floodResultBitmaps.all { it.width > 0 && it.height > 0 })
        assertTrue("MaskBitmap contains empty bitmap", maskResultBitmaps.all { it.width > 0 && it.height > 0 })

        // Persist timing output
        val outDir = File(context.cacheDir, "cutter_benchmark").apply { mkdirs() }
        File(
            outDir,
            "timing.txt",
        ).writeText(
            "Asset=${assetPath}\nBitmap=${puzzleWidth}x${puzzleHeight}\nFloodFillMs=${floodDurationMs}\nMaskBitmapMs=${maskDurationMs}\n",
        )
    }
}
