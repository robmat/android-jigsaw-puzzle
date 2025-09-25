package com.batodev.jigsawpuzzle.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.cut.PuzzleCurvesGenerator
import com.batodev.jigsawpuzzle.helpers.AdHelper
import com.batodev.jigsawpuzzle.helpers.AppRatingHelper
import com.batodev.jigsawpuzzle.helpers.FirebaseHelper
import com.batodev.jigsawpuzzle.helpers.NeonBtnOnPressChangeLook
import com.batodev.jigsawpuzzle.helpers.PlayGamesHelper
import com.batodev.jigsawpuzzle.helpers.Settings
import com.batodev.jigsawpuzzle.helpers.SettingsHelper
import com.batodev.jigsawpuzzle.logic.ImageLoader
import com.batodev.jigsawpuzzle.logic.PuzzleGameManager
import com.batodev.jigsawpuzzle.logic.PuzzleProgressListener
import com.batodev.jigsawpuzzle.logic.Stopwatch
import com.batodev.jigsawpuzzle.model.GameState
import com.batodev.jigsawpuzzle.model.PieceState
import com.batodev.jigsawpuzzle.view.PuzzlePiece
import com.batodev.jigsawpuzzle.view.TouchListener
import com.bumptech.glide.Glide
import com.caverock.androidsvg.SVG
import com.google.gson.Gson
import com.otaliastudios.zoom.ZoomLayout
import com.smb.glowbutton.NeonButton
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.compareTo
import kotlin.random.Random

const val FAKE_PROGRESS_MAX = 10
/**
 * The main activity for the puzzle game.
 */
class PuzzleActivity : AppCompatActivity(), PuzzleProgressListener {
    private var imageFileName: String? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val rateHelper: AppRatingHelper = AppRatingHelper(this)
    private lateinit var stopwatch: Stopwatch
    private lateinit var puzzleGameManager: PuzzleGameManager
    companion object {
        enum class PuzzleStatus {
            IDLE,
            CUTTING,
            SAVING
        }
        val puzzleStatus = java.util.concurrent.atomic.AtomicReference(PuzzleStatus.IDLE)
    }
    private var fakeProgress = 0

    /**
     * Called when the activity is first created.
     * Initializes the UI, loads settings, and sets up event listeners.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_puzzle)
        FirebaseHelper.logScreenView(this, "PuzzleActivity")

        val windowInsetsController =
            WindowCompat.getInsetsController(this.window, this.window.decorView)
        windowInsetsController.let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val stopWatchText = findViewById<TextView>(R.id.stopwatchText)
        stopWatchText.bringToFront()
        stopwatch = Stopwatch(stopWatchText)

        val layout = findViewById<RelativeLayout>(R.id.layout)
        val zoomableLayout = findViewById<ZoomLayout>(R.id.zoomableLayout)
        val imageView = findViewById<ImageView>(R.id.imageView)
        val settings = SettingsHelper.load(this)

        puzzleGameManager =
            PuzzleGameManager(this, layout, imageView, zoomableLayout, settings, this)

        val displayMetrics = resources.displayMetrics
        val params = layout.layoutParams
        params.width = displayMetrics.widthPixels
        params.height = displayMetrics.heightPixels
        layout.layoutParams = params
        layout.x = 0f
        layout.y = 0f

        if (intent.getBooleanExtra("newGame", false)) {
            deleteSavedGame()
        }

        val savedGameFile = File(filesDir, "saved_game/gamestate.json")
        if (savedGameFile.exists()) {
            imageView.post { loadGameState(savedGameFile) }
            imageView.post { bringMovablePiecesToFront() }
        } else {
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
                if (puzzleStatus.compareAndSet(PuzzleStatus.IDLE, PuzzleStatus.CUTTING)) {
                    puzzleGameManager.createPuzzle(bitmap, puzzlesWidth, puzzlesHeight)
                }
                fakeSomeProgress(puzzlesWidth * puzzlesHeight)
            }
        }

        findViewById<NeonButton>(R.id.puzzle_activity_play_again).let {
            it.setOnClickListener {
                FirebaseHelper.logButtonClick(this, "play_again")
                finish()
            }
            it.visibility = View.GONE
            it.setOnTouchListener { view, event ->
                NeonBtnOnPressChangeLook.neonBtnOnPressChangeLook(
                    view,
                    event,
                    this@PuzzleActivity
                )
                true
            }
        }
        rateHelper.requestReview()
    }

    /**
     * Simulates some progress on the progress bar to enhance user experience during puzzle cutting.
     * @param maxProgress The real maximum progress value to be set on the progress bar.
     */
    private fun fakeSomeProgress(maxProgress: Int) {
        for (i in 1..FAKE_PROGRESS_MAX) {
            handler.postDelayed({
                fakeProgress = i
                val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                progressBar.progress = progressBar.progress + 1
                progressBar.max = maxProgress + FAKE_PROGRESS_MAX
            }, i * 1000L + (Random.nextInt(600) - 300))
        }
    }

    private fun bringMovablePiecesToFront() {
        if (!this::puzzleGameManager.isInitialized) {
            Log.w(PuzzleActivity::class.simpleName, "bringMovablePiecesToFront: puzzleGameManager not initialized")
            return
        }

        puzzleGameManager.pieces.forEach { piece ->
            if (piece.canMove) {
                piece.bringToFront()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveGameWithProgress()
    }

    private fun saveGameWithProgress() {
        if (puzzleStatus.compareAndSet(PuzzleStatus.IDLE, PuzzleStatus.SAVING)) {
            val intent = Intent("com.batodev.jigsawpuzzle.SAVE_STARTED")
            LocalBroadcastManager.getInstance(this@PuzzleActivity).sendBroadcast(intent)
            Thread {
                try {
                    saveGameState()
                    val completeIntent = Intent("com.batodev.jigsawpuzzle.SAVE_COMPLETE")
                    LocalBroadcastManager.getInstance(this@PuzzleActivity).sendBroadcast(completeIntent)
                } finally {
                    puzzleStatus.set(PuzzleStatus.IDLE)
                }
            }.start()
        }
    }

    internal fun saveGameState() {
        if (!this::puzzleGameManager.isInitialized || puzzleGameManager.pieces.isEmpty() || puzzleGameManager.isGameOver()) {
            return
        }

        val savedGameDir = File(filesDir, "saved_game")
        if (!savedGameDir.exists()) {
            savedGameDir.mkdirs()
        }

        val pieceStates = mutableListOf<PieceState>()
        puzzleGameManager.pieces.forEachIndexed { index, piece ->
            val pieceImageFile = File(savedGameDir, "piece_$index.png")
            try {
                FileOutputStream(pieceImageFile).use { out ->
                    val bitmap = (piece.drawable as? BitmapDrawable)?.bitmap
                    bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val lParams = piece.layoutParams as RelativeLayout.LayoutParams
            val pieceState = PieceState(
                xCoord = piece.xCoord,
                yCoord = piece.yCoord,
                currentX = lParams.leftMargin,
                currentY = lParams.topMargin,
                pieceWidth = piece.pieceWidth,
                pieceHeight = piece.pieceHeight,
                canMove = piece.canMove,
                imagePath = pieceImageFile.absolutePath
            )
            pieceStates.add(pieceState)
        }

        val sourceBitmapFile = File(File(filesDir, "camera_images"), "temp.jpg")
        var savedPhotoPath: String? = null
        if (imageFileName == null && sourceBitmapFile.exists()) {
            val destFile = File(savedGameDir, "source_image.jpg")
            sourceBitmapFile.copyTo(destFile, true)
            savedPhotoPath = destFile.absolutePath
        }

        val settings = SettingsHelper.load(this)
        val gameState = GameState(
            imageFileName = imageFileName,
            photoPath = savedPhotoPath,
            puzzlesWidth = settings.lastSetDifficultyCustomWidth,
            puzzlesHeight = settings.lastSetDifficultyCustomHeight,
            elapsedTime = stopwatch.elapsedTime,
            pieces = pieceStates,
            svgString = puzzleGameManager.svgString
        )

        val gson = Gson()
        val jsonState = gson.toJson(gameState)
        val gameStateFile = File(savedGameDir, "gamestate.json")
        try {
            gameStateFile.writeText(jsonState)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.d(PuzzleActivity::class.simpleName, "Game state saved to: ${gameStateFile.absolutePath}")

        handler.post {
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            findViewById<TextView>(R.id.progressText).visibility = View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadGameState(gameStateFile: File) {
        val gson = Gson()
        val jsonState = gameStateFile.readText()
        val gameState = gson.fromJson(jsonState, GameState::class.java)

        val layout = findViewById<RelativeLayout>(R.id.layout)
        val zoomableLayout = findViewById<ZoomLayout>(R.id.zoomableLayout)
        val imageView = findViewById<ImageView>(R.id.imageView)

        stopwatch.elapsedTime = gameState.elapsedTime
        stopwatch.start()

        imageFileName = gameState.imageFileName

        val settings = SettingsHelper.load(this)
        settings.lastSetDifficultyCustomWidth = gameState.puzzlesWidth
        settings.lastSetDifficultyCustomHeight = gameState.puzzlesHeight
        SettingsHelper.save(this, settings)

        val imageLoader = ImageLoader(imageView)
        val bitmap = if (gameState.imageFileName != null) {
            imageLoader.setPicFromAsset(gameState.imageFileName, assets)
        } else {
            imageLoader.setPicFromPath(gameState.photoPath!!)
        }

        val svgString = gameState.svgString ?: run {
            val puzzleCurvesGenerator = PuzzleCurvesGenerator()
            puzzleCurvesGenerator.width = bitmap.width.toDouble()
            puzzleCurvesGenerator.height = bitmap.height.toDouble()
            puzzleCurvesGenerator.xn = gameState.puzzlesWidth.toDouble()
            puzzleCurvesGenerator.yn = gameState.puzzlesHeight.toDouble()
            puzzleCurvesGenerator.generateSvg()
        }
        puzzleGameManager.svgString = svgString

        val bitmapCopy = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(bitmapCopy)
        val paint = Paint()
        paint.alpha = 70
        if (settings.showImageInBackgroundOfThePuzzle) {
            canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint)
        }
        if (settings.showGridInBackgroundOfThePuzzle) {
            val svg = SVG.getFromString(svgString)
            svg.renderToCanvas(canvas)
        }
        imageView.setImageBitmap(bitmapCopy)

        val touchListener = TouchListener(puzzleGameManager, zoomableLayout)
        val restoredPieces = mutableListOf<PuzzlePiece>()
        gameState.pieces.forEach { pieceState ->
            val piece = PuzzlePiece(this)
            val pieceBitmap = BitmapFactory.decodeFile(pieceState.imagePath)
            piece.setImageBitmap(pieceBitmap)

            piece.xCoord = pieceState.xCoord
            piece.yCoord = pieceState.yCoord
            piece.pieceWidth = pieceState.pieceWidth
            piece.pieceHeight = pieceState.pieceHeight
            piece.canMove = pieceState.canMove

            layout.addView(piece)
            val lParams = piece.layoutParams as RelativeLayout.LayoutParams
            lParams.leftMargin = pieceState.currentX
            lParams.topMargin = pieceState.currentY
            piece.layoutParams = lParams

            if (piece.canMove) {
                piece.setOnTouchListener(touchListener)
            }
            restoredPieces.add(piece)
        }
        puzzleGameManager.pieces = restoredPieces

        findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
        findViewById<TextView>(R.id.progressText).visibility = View.GONE
    }

    /**
     * Callback for puzzle cutting progress updates.
     * Updates the progress bar and hides it when cutting is complete, then starts the stopwatch.
     * @param progress The current progress value.
     * @param max The maximum progress value.
     */
    override fun onProgressUpdate(progress: Int, max: Int) {
        handler.post {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.max = max + FAKE_PROGRESS_MAX
            progressBar.progress = progress + fakeProgress
            if (progress == max) {
                progressBar.visibility = View.GONE
                findViewById<TextView>(R.id.progressText).visibility = View.GONE
                stopwatch.start()
            }
        }
    }

    /**
     * Callback indicating that the puzzle cutting process has finished.
     * Triggers the scattering of puzzle pieces on the game board.
     */
    override fun onCuttingFinished() {
        puzzleStatus.set(PuzzleStatus.IDLE)
        puzzleGameManager.scatterPieces()
    }

    /**
     * Handles the game over state.
     * Displays confetti, adds the uncovered image to the gallery, stops the stopwatch,
     * plays a win sound, shows the play again button, and displays an ad.
     * Also updates and shows high scores.
     * @see AdHelper
     * @see SettingsHelper
     * @see Stopwatch
     * @see PuzzleGameManager
     */
    @SuppressLint("ClickableViewAccessibility")
    fun onGameOver() {
        FirebaseHelper.logEvent(this, "game_over")
        val settings = handleAchievements()

        val konfetti = findViewById<ImageView>(R.id.konfettiView)
        Glide.with(konfetti).asGif().load(R.drawable.confetti2).into(konfetti)
        konfetti.visibility = View.VISIBLE
        imageFileName?.let {
            if (!settings.uncoveredPics.contains(it)) {
                settings.uncoveredPics.add(it)
            }
            SettingsHelper.save(this, settings)
            Toast.makeText(this, R.string.image_added_to_gallery, Toast.LENGTH_SHORT).show()
        }
        stopwatch.stop()
        puzzleGameManager.playWinSound()
        findViewById<NeonButton>(R.id.puzzle_activity_play_again).let {
            it.visibility = View.VISIBLE
            it.setOnTouchListener { view, event ->
                NeonBtnOnPressChangeLook.neonBtnOnPressChangeLook(view, event, this@PuzzleActivity)
                true
            }
        }
        AdHelper.showAd(this)

        val elapsedTime = stopwatch.elapsedTime
        val difficultyKey =
            "${settings.lastSetDifficultyCustomWidth}x${settings.lastSetDifficultyCustomHeight}"
        updateAndShowHighScores(elapsedTime, difficultyKey, settings)

        deleteSavedGame()
    }

    private fun deleteSavedGame() {
        val savedGameDir = File(filesDir, "saved_game")
        if (savedGameDir.exists()) {
            savedGameDir.deleteRecursively()
        }
    }

    private fun handleAchievements(): Settings {
        PlayGamesHelper.unlockAchievement(this, R.string.achievement_puzzle_initiate)
        val settings = SettingsHelper.load(this)
        val totalPieces =
            settings.lastSetDifficultyCustomWidth * settings.lastSetDifficultyCustomHeight
        if (totalPieces < 20) {
            PlayGamesHelper.unlockAchievement(this, R.string.achievement_quick_game)
        }
        if (intent.getStringExtra(PhotoSource::class.simpleName)?.equals(PhotoSource.CAMERA.name) == true) {
            PlayGamesHelper.unlockAchievement(this, R.string.achievement_photographer)
        }
        if (intent.getStringExtra(PhotoSource::class.simpleName)?.equals(PhotoSource.GALLERY.name) == true) {
            PlayGamesHelper.unlockAchievement(this, R.string.achievement_curator)
        }
        PlayGamesHelper.progressAchievement(this, R.string.achievement_apprentice, 1)
        return settings
    }

    /**
     * Updates the high scores for a given difficulty and displays the high score popup.
     * If the new score is among the top 10, a congratulatory toast is shown.
     * @param newTime The elapsed time for the current puzzle solution in seconds.
     * @param difficultyKey A string representing the puzzle difficulty (e.g., "3x5").
     * @param settings The current {@link Settings} object containing high scores.
     * @see Settings
     * @see SettingsHelper
     */
    private fun updateAndShowHighScores(newTime: Int, difficultyKey: String, settings: Settings) {
        val highScores = settings.highscores.getOrPut(difficultyKey) { mutableListOf() }

        val currentScoreInSeconds = newTime
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val newScoreString = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            currentScoreInSeconds / 60,
            currentScoreInSeconds % 60
        ) +
                " - " + dateFormat.format(Date())

        highScores.add(newScoreString)

        // Sort by time (first part of the string)
        highScores.sortBy {
            val parts = it.split(" - ")
            val timeParts = parts[0].split(":")
            timeParts[0].toInt() * 60 + timeParts[1].toInt()
        }
        while (highScores.size > 10) {
            highScores.removeAt(10)
        }

        SettingsHelper.save(this, settings)

        showHighScorePopup(difficultyKey, highScores, highScores.indexOf(newScoreString))

        if (highScores.indexOf(newScoreString) <= 10 && highScores.indexOf(newScoreString) != -1) {
            FirebaseHelper.logEvent(this, "new_highscore")
            Toast.makeText(this, getString(R.string.congratulations_top_10), Toast.LENGTH_LONG)
                .show()
        }
    }

    /**
     * Displays the high score popup with the top 10 scores for a given difficulty.
     * The new score, if it made it into the top 10, is highlighted in bold.
     * @param difficultyKey A string representing the puzzle difficulty (e.g., "3x5").
     * @param highScores The list of high score strings to display.
     * @param newScoreIndex The index of the newly achieved score in the highScores list, or -1 if not in top 10.
     */
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun showHighScorePopup(
        difficultyKey: String,
        highScores: MutableList<String>,
        newScoreIndex: Int,
    ) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.high_score_popup, null)

        val highScoreDifficulty = popupView.findViewById<TextView>(R.id.highScoreDifficulty)
        val highScoreListContainer =
            popupView.findViewById<LinearLayout>(R.id.highScoreListContainer)

        highScoreDifficulty.text = difficultyKey

        // Populate high scores
        for ((index, scoreString) in highScores.withIndex()) {
            val scoreTextView = TextView(this)
            scoreTextView.setTextColor(resources.getColor(R.color.white, null))
            scoreTextView.text = "${index + 1}. $scoreString"
            scoreTextView.textSize = 16f // Use 16f for sp
            if (index == newScoreIndex) {
                scoreTextView.setTypeface(null, Typeface.BOLD)
            }
            highScoreListContainer.addView(scoreTextView)
        }

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(popupView)
        builder.setCancelable(false)

        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        alertDialog.show()

        popupView.findViewById<NeonButton>(R.id.highScoreOkButton).let {
            it.setOnClickListener {
                FirebaseHelper.logButtonClick(this, "highscore_ok")
                alertDialog.dismiss()
            }
            it.setOnTouchListener { view, event ->
                NeonBtnOnPressChangeLook.neonBtnOnPressChangeLook(view, event, this@PuzzleActivity)
                true
            }
        }
    }

    /**
     * Posts a {@link Runnable} to the main thread's handler.
     * This is used for UI updates that need to be performed on the main thread.
     * @param r The {@link Runnable} to be executed.
     */
    override fun postToHandler(r: Runnable) {
        handler.post(r)
    }
}
