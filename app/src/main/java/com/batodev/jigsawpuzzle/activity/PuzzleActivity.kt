package com.batodev.jigsawpuzzle.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.helpers.AdHelper
import com.batodev.jigsawpuzzle.helpers.AppRatingHelper
import com.batodev.jigsawpuzzle.helpers.SettingsHelper
import com.batodev.jigsawpuzzle.logic.ImageLoader
import com.batodev.jigsawpuzzle.logic.PuzzleGameManager
import com.batodev.jigsawpuzzle.logic.PuzzleProgressListener
import com.batodev.jigsawpuzzle.logic.Stopwatch
import com.bumptech.glide.Glide
import com.otaliastudios.zoom.ZoomLayout
import java.io.File

class PuzzleActivity : AppCompatActivity(), PuzzleProgressListener {
    private var imageFileName: String? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val rateHelper: AppRatingHelper = AppRatingHelper(this)
    private lateinit var stopwatch: Stopwatch
    private lateinit var puzzleGameManager: PuzzleGameManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_puzzle)

        val windowInsetsController = WindowCompat.getInsetsController(this.window, this.window.decorView)
        windowInsetsController.let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val stopWatchText = findViewById<TextView>(R.id.stopwatchText)
        stopWatchText.bringToFront()
        stopwatch = Stopwatch(stopWatchText)

        val layout = findViewById<RelativeLayout>(R.id.layout)
        val zoomableLayout = findViewById<ZoomLayout>(R.id.zoomableLayout)
        val imageView = findViewById<ImageView>(R.id.imageView)
        val settings = SettingsHelper.load(this)

        puzzleGameManager = PuzzleGameManager(this, layout, imageView, zoomableLayout, settings, this)

        val displayMetrics = resources.displayMetrics
        val params = layout.layoutParams
        params.width = displayMetrics.widthPixels
        params.height = displayMetrics.heightPixels
        layout.layoutParams = params
        layout.x = 0f
        layout.y = 0f

        imageFileName = intent.getStringExtra("assetName")
        val puzzlesWidth = settings.lastSetDifficultyCustomWidth
        val puzzlesHeight = settings.lastSetDifficultyCustomHeight

        imageView.post {
            val imageLoader = ImageLoader(imageView)
            val bitmap = if (imageFileName != null) {
                imageLoader.setPicFromAsset(imageFileName!!, assets)
            } else {
                val photoPath = File(File(filesDir, "camera_images"), "temp.jpg").toString()
                imageLoader.setPicFromPath(photoPath)
            }
            puzzleGameManager.createPuzzle(bitmap, puzzlesWidth, puzzlesHeight)
        }

        findViewById<Button>(R.id.puzzle_activity_play_again).setOnClickListener {
            finish()
        }
        rateHelper.requestReview()
    }

    override fun onProgressUpdate(progress: Int, max: Int) {
        handler.post {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.max = max
            progressBar.progress = progress
            if (progress == max) {
                progressBar.visibility = View.GONE
                findViewById<TextView>(R.id.progressText).visibility = View.GONE
                stopwatch.start()
            }
        }
    }

    override fun onCuttingFinished() {
        puzzleGameManager.scatterPieces()
    }

    fun onGameOver() {
        val konfetti = findViewById<ImageView>(R.id.konfettiView)
        Glide.with(konfetti).asGif().load(R.drawable.confetti2).into(konfetti)
        konfetti.visibility = View.VISIBLE
        val settings = SettingsHelper.load(this)
        imageFileName?.let {
            settings.uncoveredPics.add(it)
            SettingsHelper.save(this, settings)
            Toast.makeText(this, R.string.image_added_to_gallery, Toast.LENGTH_SHORT).show()
        }
        stopwatch.stop()
        puzzleGameManager.playWinSound()
        findViewById<Button>(R.id.puzzle_activity_play_again).visibility = View.VISIBLE
        AdHelper.showAd(this)
    }

    override fun postToHandler(r: Runnable) {
        handler.post(r)
    }
}
