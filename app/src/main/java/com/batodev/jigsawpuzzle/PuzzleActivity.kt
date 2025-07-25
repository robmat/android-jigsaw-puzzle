package com.batodev.jigsawpuzzle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.batodev.jigsawpuzzle.cut.PuzzleCurvesGenerator
import com.batodev.jigsawpuzzle.cut.PuzzleCutter
import com.bumptech.glide.Glide
import com.caverock.androidsvg.SVG
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Random
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class PuzzleActivity : AppCompatActivity() {
    private var puzzlesHeight: Int = 4
    private var puzzlesWidth: Int = 3
    private var pieces: MutableList<PuzzlePiece> = mutableListOf()
    private var imageFileName: String? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val rateHelper: AppRatingHelper = AppRatingHelper(this)
    private val winSoundIds = listOf(
        R.raw.success_1,
        R.raw.success_2,
        R.raw.success_3,
        R.raw.success_4,
        R.raw.success_5,
        R.raw.success_6
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_puzzle)

        val windowInsetsController = WindowCompat.getInsetsController(this.window, this.window.decorView)
        windowInsetsController.let { controller ->
            // Hide both bars
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // Sticky behavior - bars stay hidden until user swipes
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val layout = findViewById<RelativeLayout>(R.id.layout)
        val imageView = findViewById<ImageView>(R.id.imageView)
        val settings = SettingsHelper.load(this)
        val intent = intent
        imageFileName = intent.getStringExtra("assetName")
        puzzlesWidth = settings.lastSetDifficultyCustomWidth
        puzzlesHeight = settings.lastSetDifficultyCustomHeight

        // run image related code after the view was laid out
        // to have all dimensions calculated
        imageView.post {
            if (imageFileName != null) {
                setPicFromAsset(imageFileName!!, imageView)
            } else if (intent.getStringExtra("mCurrentPhotoPath") != null) {
                val time = measureTimeMillis {
                    setPicFromPath(imageView)
                }
                Log.d(PuzzleActivity::class.java.simpleName, "setPicFromPath took: $time ms")
            }
            pieces = splitImage(settings)
            val touchListener = TouchListener(this@PuzzleActivity)
            // shuffle pieces order
            pieces.shuffle()
            for (piece in pieces) {
                piece.setOnTouchListener(touchListener)
                layout.addView(piece)
                // randomize position, on the bottom of the screen
                val lParams = piece.layoutParams as RelativeLayout.LayoutParams
                lParams.leftMargin = Random().nextInt(layout.width - piece.pieceWidth)
                lParams.topMargin = layout.height - piece.pieceHeight - Random().nextInt(300)
                piece.layoutParams = lParams
            }
        }

        rateHelper.requestReview()
    }

    fun updateProgress(progress: Int, max: Int) {
        handler.post {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.max = max
            progressBar.progress = progress
            if (progress == max) {
                progressBar.visibility = View.GONE
                findViewById<TextView>(R.id.progressText).visibility = View.GONE
            }
        }
    }

    private fun setPicFromAsset(assetName: String, imageView: ImageView) {
        // Get the dimensions of the View
        val targetW = imageView.width
        val targetH = imageView.height
        val am = assets
        try {
            val inputStream = am.open("img/$assetName")
            // Get the dimensions of the bitmap
            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, Rect(-1, -1, -1, -1), bmOptions)
            val photoW = bmOptions.outWidth
            val photoH = bmOptions.outHeight

            // Determine how much to scale down the image
            val scaleFactor = (photoW / targetW).coerceAtMost(photoH / targetH)
            inputStream.reset()

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false
            bmOptions.inSampleSize = scaleFactor
            val bitmap = BitmapFactory.decodeStream(inputStream, Rect(-1, -1, -1, -1), bmOptions)
            imageView.setImageBitmap(bitmap)
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun splitImage(settings: Settings): MutableList<PuzzlePiece> {
        val rows = puzzlesHeight
        val cols = puzzlesWidth
        val imageView = findViewById<ImageView>(R.id.imageView)
        val pieces = mutableListOf<PuzzlePiece>()

        // Get the scaled bitmap of the source image
        val drawable = imageView.drawable as BitmapDrawable
        val bitmap = drawable.bitmap
        val dimensions = getBitmapPositionInsideImageView(imageView)
        val scaledBitmapLeft = dimensions[0]
        val scaledBitmapTop = dimensions[1]
        val scaledBitmapWidth = dimensions[2]
        val scaledBitmapHeight = dimensions[3]
        val croppedImageWidth = (scaledBitmapWidth)
        val croppedImageHeight = (scaledBitmapHeight)
        val scaledBitmap =
            bitmap.scale(scaledBitmapWidth, scaledBitmapHeight)
        val croppedBitmap = Bitmap.createBitmap(
            scaledBitmap,
            abs(scaledBitmapLeft),
            abs(scaledBitmapTop),
            croppedImageWidth,
            croppedImageHeight
        )

        val puzzleCurvesGenerator = PuzzleCurvesGenerator()
        puzzleCurvesGenerator.width = croppedBitmap.width.toDouble()
        puzzleCurvesGenerator.height = croppedBitmap.height.toDouble()
        puzzleCurvesGenerator.xn = cols.toDouble()
        puzzleCurvesGenerator.yn = rows.toDouble()
        val svgString = puzzleCurvesGenerator.generateSvg()
        // paint grid on image
        val bitmapCopy =
            createBitmap(croppedBitmap.width, croppedBitmap.height)
        val canvas = Canvas(bitmapCopy)
        val paint = Paint()
        paint.alpha = 70
        if (settings.showImageInBackgroundOfThePuzzle) {
            canvas.drawBitmap(croppedBitmap, 0.0f, 0.0f, paint)
        }
        if (settings.showGridInBackgroundOfThePuzzle) {
            val svg = SVG.getFromString(svgString)
            svg.renderToCanvas(canvas)
        }
        imageView.setImageBitmap(bitmapCopy)

        // Calculate the with and height of the pieces
        val pieceWidth = croppedImageWidth / cols
        val pieceHeight = croppedImageHeight / rows

        // Create each bitmap piece and add it to the resulting array
        var yCoord = 0
        for (row in 0 until rows) {
            var xCoord = 0
            for (col in 0 until cols) {
                // calculate offset for each piece
                var offsetX = 0
                var offsetY = 0
                if (col > 0) {
                    offsetX = pieceWidth / 3
                }
                if (row > 0) {
                    offsetY = pieceHeight / 3
                }

                val piece = PuzzlePiece(this)
                piece.xCoord = xCoord - offsetX + imageView.left + 4
                piece.yCoord = yCoord - offsetY + imageView.top + 7
                piece.pieceWidth = pieceWidth + offsetX
                piece.pieceHeight = pieceHeight + offsetY
                pieces.add(piece)
                xCoord += pieceWidth
            }
            yCoord += pieceHeight
        }
        PuzzleCutter.cut(croppedBitmap, rows, cols, svgString, imageView, this, pieces)
        return pieces
    }

    private fun getBitmapPositionInsideImageView(imageView: ImageView?): IntArray {
        val ret = IntArray(4)
        if (imageView == null || imageView.drawable == null) return ret

        // Get image dimensions
        // Get image matrix values and place them in an array
        val f = FloatArray(9)
        imageView.imageMatrix.getValues(f)

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        val scaleX = f[Matrix.MSCALE_X]
        val scaleY = f[Matrix.MSCALE_Y]

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        val d = imageView.drawable
        val origW = d.intrinsicWidth
        val origH = d.intrinsicHeight

        // Calculate the actual dimensions
        val actW = (origW * scaleX).roundToInt()
        val actH = (origH * scaleY).roundToInt()
        ret[2] = actW
        ret[3] = actH

        // Get image position
        // We assume that the image is centered into ImageView
        ret[0] = 0//left
        ret[1] = 0//top
        return ret
    }

    fun checkGameOver() {
        if (isGameOver) {
            val konfetti = findViewById<ImageView>(R.id.konfettiView)
            Glide
                .with(konfetti)
                .asGif()
                .load(R.drawable.confetti2)
                .into(konfetti)
            konfetti.visibility = View.VISIBLE
            val settings = SettingsHelper.load(this)
            imageFileName?.let {
                settings.uncoveredPics.add(it)
                SettingsHelper.save(this, settings)
                Toast.makeText(this, R.string.image_added_to_gallery, Toast.LENGTH_SHORT).show()
            }
            SoundsPlayer.play(winSoundIds.random(), this)
            findViewById<Button>(R.id.puzzle_activity_play_again).visibility = View.VISIBLE
            AdHelper.showAd(this)
        }
    }

    private val isGameOver: Boolean
        get() {
            for (piece in pieces) {
                if (piece.canMove) {
                    return false
                }
            }
            return true
        }

    private fun setPicFromPath(imageView: ImageView) {
        val targetW = imageView.width
        val targetH = imageView.height
        val mCurrentPhotoPath = File(File(filesDir, "camera_images"), "temp.jpg").toString()
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight

        // Determine how much to scale down the image
        val scaleFactor = (photoW / targetW).coerceAtMost(photoH / targetH)

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor
        val bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)
        val rotatedBitmap = uprightBitmap(bitmap, mCurrentPhotoPath)
        val croppedBitmap = cropToAspectRatio(rotatedBitmap)

        imageView.setImageBitmap(croppedBitmap)
    }

    private fun uprightBitmap(bitmap: Bitmap, imagePath: String): Bitmap {
        val exifInterface = ExifInterface(imagePath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun cropToAspectRatio(bitmap: Bitmap): Bitmap {
        val aspectRatio: BigDecimal = BigDecimal(2).divide(BigDecimal(3), 5, RoundingMode.HALF_UP)
        val width = BigDecimal(bitmap.width)
        val height = BigDecimal(bitmap.height)

        val targetWidth: Int
        val targetHeight: Int

        // Calculate the dimensions of the cropped region based on the aspect ratio
        if (width / height > aspectRatio) {
            targetWidth = (height * aspectRatio).toInt()
            targetHeight = height.toInt()
        } else {
            targetWidth = width.toInt()
            targetHeight = (width / aspectRatio).toInt()
        }

        // Calculate the coordinates of the top-left corner of the cropped region
        val left = (width.toInt() - targetWidth) / 2
        val top = (height.toInt() - targetHeight) / 2

        // Create a Rect object representing the cropping region
        val rect = Rect(left, top, left + targetWidth, top + targetHeight)

        // Crop the bitmap to the specified region
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    }

    fun playAgain(view: View) {
        finish()
    }

    fun postToHandler(r: Runnable) {
        handler.post(r)
    }
}
