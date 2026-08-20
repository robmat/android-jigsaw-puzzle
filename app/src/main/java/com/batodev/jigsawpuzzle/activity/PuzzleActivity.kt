package com.batodev.jigsawpuzzle.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.cut.PuzzleCurvesGenerator
import com.batodev.jigsawpuzzle.helpers.AchievementHelper
import com.batodev.jigsawpuzzle.helpers.AdHelper
import com.batodev.jigsawpuzzle.helpers.AppRatingHelper
import com.batodev.jigsawpuzzle.helpers.FirebaseHelper
import com.batodev.jigsawpuzzle.helpers.NeonBtnOnPressChangeLook
import com.batodev.jigsawpuzzle.helpers.PlayGamesHelper
import com.batodev.jigsawpuzzle.helpers.Settings
import com.batodev.jigsawpuzzle.helpers.SettingsHelper
import com.batodev.jigsawpuzzle.logic.ImageLoader
import com.batodev.jigsawpuzzle.logic.PuzzleBackgroundRenderer
import com.batodev.jigsawpuzzle.logic.PuzzleGameManager
import com.batodev.jigsawpuzzle.logic.PuzzleProgressListener
import com.batodev.jigsawpuzzle.logic.Stopwatch
import com.batodev.jigsawpuzzle.model.GameState
import com.batodev.jigsawpuzzle.model.PieceState
import com.batodev.jigsawpuzzle.view.PuzzlePiece
import com.batodev.jigsawpuzzle.view.TouchListener
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.otaliastudios.zoom.ZoomLayout
import com.smb.glowbutton.NeonButton
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

const val FAKE_PROGRESS_MAX = 10

/**
 * The main activity for the puzzle game.
 */
class PuzzleActivity :
    AppCompatActivity(),
    PuzzleProgressListener {
    internal var imageFileName: String? = null
    internal val handler: Handler = Handler(Looper.getMainLooper())
    private val rateHelper: AppRatingHelper = AppRatingHelper(this)
    internal lateinit var stopwatch: Stopwatch
    internal lateinit var puzzleGameManager: PuzzleGameManager
    private val gameStateController = GameStateController(this)
    private val completionFlow = PuzzleCompletionFlow(this)

    companion object {
        enum class PuzzleStatus {
            IDLE,
            CUTTING,
            SAVING,
        }

        val puzzleStatus =
            java.util.concurrent.atomic
                .AtomicReference(PuzzleStatus.IDLE)

        private const val FAKE_PROGRESS_BASE_DELAY_MS = 1000L
        private const val FAKE_PROGRESS_JITTER_RANGE = 600
        private const val FAKE_PROGRESS_JITTER_OFFSET = 300
        private const val MARATHONER_SECONDS_THRESHOLD = 3600
        private const val ACHIEVEMENT_PROGRESS_STEP = 1
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

        fun hideSystemBars() {
            val windowInsetsController = WindowCompat.getInsetsController(this.window, this.window.decorView)
            windowInsetsController.let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        hideSystemBars()

        val stopWatchText = findViewById<TextView>(R.id.stopwatchText)
        stopWatchText.bringToFront()
        stopwatch = Stopwatch(stopWatchText)

        val layout = findViewById<RelativeLayout>(R.id.layout)
        val zoomableLayout = findViewById<ZoomLayout>(R.id.zoomableLayout)
        val imageView = findViewById<ImageView>(R.id.imageView)
        val settings = SettingsHelper.load(this)

        puzzleGameManager =
            PuzzleGameManager(this, layout, imageView, zoomableLayout, settings, this)

        fun setupLayoutDimensions() {
            val displayMetrics = resources.displayMetrics
            val params = layout.layoutParams
            params.width = displayMetrics.widthPixels
            params.height = displayMetrics.heightPixels
            layout.layoutParams = params
            layout.x = 0f
            layout.y = 0f
        }
        setupLayoutDimensions()

        if (intent.getBooleanExtra("newGame", false)) {
            gameStateController.deleteSaved()
        }

        resumeOrStartNewGame(imageView, settings)

        // Local (not a class member) so it doesn't add to this activity's function count -
        // it's only ever called once, right below.
        fun setupPlayAgainButton() {
            findViewById<NeonButton>(R.id.puzzle_activity_play_again).let {
                it.setOnClickListener {
                    FirebaseHelper.logButtonClick(this, "play_again")
                    finish()
                }
                it.visibility = View.GONE
                it.setOnTouchListener { view, event ->
                    NeonBtnOnPressChangeLook.applyPressedLook(
                        view,
                        event,
                        this@PuzzleActivity,
                    )
                    true
                }
            }
        }
        setupPlayAgainButton()

        rateHelper.requestReview()
        AchievementHelper.updateDailyRitualAchievement(this)
    }

    /**
     * Whether [puzzleGameManager] has been assigned yet. Exposed as a member
     * function (rather than a top-level helper) because Kotlin only allows
     * checking a lateinit property's `isInitialized` from code with lexical
     * access to it: this class, an outer class, or top level in this file.
     */
    internal fun isPuzzleGameManagerReady(): Boolean = this::puzzleGameManager.isInitialized

    private fun resumeOrStartNewGame(
        imageView: ImageView,
        settings: Settings,
    ) {
        val savedGameFile = File(filesDir, "saved_game/gamestate.json")
        if (savedGameFile.exists()) {
            imageView.post { gameStateController.load(savedGameFile) }
            imageView.post { bringMovablePiecesToFront() }
        } else {
            imageFileName = intent.getStringExtra("assetName")
            val puzzlesWidth = settings.lastSetDifficultyCustomWidth
            val puzzlesHeight = settings.lastSetDifficultyCustomHeight

            imageView.post {
                val imageLoader = ImageLoader(imageView)
                val bitmap =
                    if (imageFileName != null) {
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
    }

    /**
     * Simulates some progress on the progress bar to enhance user experience during puzzle cutting.
     * @param maxProgress The real maximum progress value to be set on the progress bar.
     */
    private fun fakeSomeProgress(maxProgress: Int) {
        for (i in 1..FAKE_PROGRESS_MAX) {
            val jitter = Random.nextInt(FAKE_PROGRESS_JITTER_RANGE) - FAKE_PROGRESS_JITTER_OFFSET
            val delay = i * FAKE_PROGRESS_BASE_DELAY_MS + jitter
            handler.postDelayed({
                fakeProgress = i
                val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                progressBar.progress = progressBar.progress + 1
                progressBar.max = maxProgress + FAKE_PROGRESS_MAX
            }, delay)
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
        gameStateController.saveWithProgress()
    }

    /**
     * Callback for puzzle cutting progress updates.
     * Updates the progress bar and hides it when cutting is complete, then starts the stopwatch.
     * @param progress The current progress value.
     * @param max The maximum progress value.
     */
    override fun onProgressUpdate(
        progress: Int,
        max: Int,
    ) {
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
        val elapsedTime = stopwatch.elapsedTime
        val settings = completionFlow.handleAchievements(elapsedTime)

        val konfetti = findViewById<ImageView>(R.id.konfettiView)
        Glide
            .with(konfetti)
            .asGif()
            .load(R.drawable.confetti2)
            .into(konfetti)
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
                NeonBtnOnPressChangeLook.applyPressedLook(view, event, this@PuzzleActivity)
                true
            }
        }
        AdHelper.showAd(this)

        val difficultyKey =
            "${settings.lastSetDifficultyCustomWidth}x${settings.lastSetDifficultyCustomHeight}"
        completionFlow.updateAndShowHighScores(elapsedTime, difficultyKey, settings)
        gameStateController.deleteSaved()
        settings.marathonerPlaytime += elapsedTime
        Log.d(
            PuzzleActivity::class.simpleName,
            "Total playtime: ${settings.marathonerPlaytime} seconds, elapsed this game: $elapsedTime seconds",
        )
        if (settings.marathonerPlaytime >= MARATHONER_SECONDS_THRESHOLD) {
            settings.marathonerPlaytime = 0
            PlayGamesHelper.progressAchievement(this, R.string.achievement_marathoner, ACHIEVEMENT_PROGRESS_STEP)
        }
        SettingsHelper.save(this, settings)
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

/**
 * Owns saving/loading the in-progress game to disk, split out of [PuzzleActivity]
 * so that class only holds the puzzle-screen lifecycle and callbacks.
 */
private class GameStateController(
    private val activity: PuzzleActivity,
) {
    companion object {
        private const val PNG_COMPRESS_QUALITY = 100
    }

    fun saveWithProgress() {
        val status = PuzzleActivity.Companion.puzzleStatus
        val idle = PuzzleActivity.Companion.PuzzleStatus.IDLE
        val saving = PuzzleActivity.Companion.PuzzleStatus.SAVING
        if (status.compareAndSet(idle, saving)) {
            val intent = Intent("com.batodev.jigsawpuzzle.SAVE_STARTED")
            LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)
            Thread {
                try {
                    save()
                    val completeIntent = Intent("com.batodev.jigsawpuzzle.SAVE_COMPLETE")
                    LocalBroadcastManager.getInstance(activity).sendBroadcast(completeIntent)
                } finally {
                    status.set(idle)
                }
            }.start()
        }
    }

    fun save() {
        if (!activity.isPuzzleGameManagerReady() ||
            activity.puzzleGameManager.pieces.isEmpty() ||
            activity.puzzleGameManager.isGameOver()
        ) {
            return
        }

        val savedGameDir = File(activity.filesDir, "saved_game")
        if (!savedGameDir.exists()) {
            savedGameDir.mkdirs()
        }

        val pieceStates = serializePieces(savedGameDir)

        val sourceBitmapFile = File(File(activity.filesDir, "camera_images"), "temp.jpg")
        var savedPhotoPath: String? = null
        if (activity.imageFileName == null && sourceBitmapFile.exists()) {
            val destFile = File(savedGameDir, "source_image.jpg")
            sourceBitmapFile.copyTo(destFile, true)
            savedPhotoPath = destFile.absolutePath
        }

        val settings = SettingsHelper.load(activity)
        val gameState =
            GameState(
                imageFileName = activity.imageFileName,
                photoPath = savedPhotoPath,
                puzzlesWidth = settings.lastSetDifficultyCustomWidth,
                puzzlesHeight = settings.lastSetDifficultyCustomHeight,
                elapsedTime = activity.stopwatch.elapsedTime,
                pieces = pieceStates,
                svgString = activity.puzzleGameManager.svgString,
            )
        writeGameStateFile(savedGameDir, gameState)

        activity.handler.post {
            activity.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            activity.findViewById<TextView>(R.id.progressText).visibility = View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun load(gameStateFile: File) {
        PlayGamesHelper.unlockAchievement(activity, R.string.achievement_welcome_back)
        val gson = Gson()
        val jsonState = gameStateFile.readText()
        val gameState = gson.fromJson(jsonState, GameState::class.java)

        val layout = activity.findViewById<RelativeLayout>(R.id.layout)
        val zoomableLayout = activity.findViewById<ZoomLayout>(R.id.zoomableLayout)
        val imageView = activity.findViewById<ImageView>(R.id.imageView)

        activity.stopwatch.elapsedTime = gameState.elapsedTime
        activity.stopwatch.start()

        activity.imageFileName = gameState.imageFileName

        val settings = SettingsHelper.load(activity)
        settings.lastSetDifficultyCustomWidth = gameState.puzzlesWidth
        settings.lastSetDifficultyCustomHeight = gameState.puzzlesHeight
        SettingsHelper.save(activity, settings)

        val imageLoader = ImageLoader(imageView)
        val bitmap =
            if (gameState.imageFileName != null) {
                imageLoader.setPicFromAsset(gameState.imageFileName, activity.assets)
            } else {
                imageLoader.setPicFromPath(gameState.photoPath!!)
            }

        val svgString = gameState.svgString ?: generateSvgString(bitmap, gameState)
        activity.puzzleGameManager.svgString = svgString

        PuzzleBackgroundRenderer.draw(imageView, bitmap, svgString, settings)
        activity.puzzleGameManager.pieces = restorePieces(layout, zoomableLayout, gameState)

        activity.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
        activity.findViewById<TextView>(R.id.progressText).visibility = View.GONE
    }

    fun deleteSaved() {
        val savedGameDir = File(activity.filesDir, "saved_game")
        if (savedGameDir.exists()) {
            savedGameDir.deleteRecursively()
        }
    }

    private fun serializePieces(savedGameDir: File): List<PieceState> {
        val pieceStates = mutableListOf<PieceState>()
        activity.puzzleGameManager.pieces.forEachIndexed { index, piece ->
            val pieceImageFile = File(savedGameDir, "piece_$index.png")
            try {
                FileOutputStream(pieceImageFile).use { out ->
                    val bitmap = (piece.drawable as? BitmapDrawable)?.bitmap
                    bitmap?.compress(Bitmap.CompressFormat.PNG, PNG_COMPRESS_QUALITY, out)
                }
            } catch (e: IOException) {
                FirebaseHelper.logException(activity, "serializePieces", e.message)
                Log.w(GameStateController::class.simpleName, "Error writing piece image $index", e)
            }

            val lParams = piece.layoutParams as RelativeLayout.LayoutParams
            pieceStates.add(
                PieceState(
                    xCoord = piece.xCoord,
                    yCoord = piece.yCoord,
                    currentX = lParams.leftMargin,
                    currentY = lParams.topMargin,
                    pieceWidth = piece.pieceWidth,
                    pieceHeight = piece.pieceHeight,
                    canMove = piece.canMove,
                    imagePath = pieceImageFile.absolutePath,
                ),
            )
        }
        return pieceStates
    }

    private fun writeGameStateFile(
        savedGameDir: File,
        gameState: GameState,
    ) {
        val gson = Gson()
        val jsonState = gson.toJson(gameState)
        val gameStateFile = File(savedGameDir, "gamestate.json")
        try {
            gameStateFile.writeText(jsonState)
        } catch (e: IOException) {
            FirebaseHelper.logException(activity, "writeGameStateFile", e.message)
            Log.w(GameStateController::class.simpleName, "Error writing game state file", e)
        }
        Log.d(GameStateController::class.simpleName, "Game state saved to: ${gameStateFile.absolutePath}")
    }

    private fun generateSvgString(
        bitmap: Bitmap,
        gameState: GameState,
    ): String {
        val puzzleCurvesGenerator = PuzzleCurvesGenerator()
        puzzleCurvesGenerator.width = bitmap.width.toDouble()
        puzzleCurvesGenerator.height = bitmap.height.toDouble()
        puzzleCurvesGenerator.xn = gameState.puzzlesWidth.toDouble()
        puzzleCurvesGenerator.yn = gameState.puzzlesHeight.toDouble()
        return puzzleCurvesGenerator.generateSvg()
    }

    private fun restorePieces(
        layout: RelativeLayout,
        zoomableLayout: ZoomLayout,
        gameState: GameState,
    ): MutableList<PuzzlePiece> {
        val touchListener = TouchListener(activity.puzzleGameManager, zoomableLayout)
        val restoredPieces = mutableListOf<PuzzlePiece>()
        gameState.pieces.forEach { pieceState ->
            val piece = PuzzlePiece(activity)
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
        return restoredPieces
    }
}

/**
 * Owns the end-of-game achievement checks and the high-score popup, split out
 * of [PuzzleActivity] so that class only holds the puzzle-screen lifecycle and
 * callbacks.
 */
private class PuzzleCompletionFlow(
    private val activity: PuzzleActivity,
) {
    companion object {
        private const val SECONDS_PER_MINUTE = 60
        private const val MAX_HIGH_SCORES = 10
        private const val HIGH_SCORE_TEXT_SIZE_SP = 16f
        private const val QUICK_GAME_PIECE_THRESHOLD = 20
        private const val SPEEDSTER_PIECE_THRESHOLD = 50
        private const val SPEEDSTER_TIME_THRESHOLD_SECONDS = 180
        private const val LARGE_PUZZLE_PIECE_THRESHOLD = 100
        private const val NIGHT_OWL_HOUR_END = 4
        private const val ACHIEVEMENT_PROGRESS_STEP = 1
    }

    fun handleAchievements(elapsedTime: Int): Settings {
        PlayGamesHelper.unlockAchievement(activity, R.string.achievement_puzzle_initiate)
        val settings = SettingsHelper.load(activity)
        val totalPieces =
            settings.lastSetDifficultyCustomWidth * settings.lastSetDifficultyCustomHeight
        if (totalPieces < QUICK_GAME_PIECE_THRESHOLD) {
            PlayGamesHelper.unlockAchievement(activity, R.string.achievement_quick_game)
        }
        if (totalPieces >= SPEEDSTER_PIECE_THRESHOLD && elapsedTime < SPEEDSTER_TIME_THRESHOLD_SECONDS) {
            PlayGamesHelper.unlockAchievement(activity, R.string.achievement_speedster)
        }
        if (totalPieces > LARGE_PUZZLE_PIECE_THRESHOLD) {
            PlayGamesHelper.unlockAchievement(activity, R.string.achievement_the_big_picture)
        }
        if (totalPieces >= MAX_COLUMNS * MAX_ROWS) {
            PlayGamesHelper.unlockAchievement(activity, R.string.achievement_maximum_effort)
        }
        val isMinimalist = !settings.showImageInBackgroundOfThePuzzle && !settings.showGridInBackgroundOfThePuzzle
        if (totalPieces >= LARGE_PUZZLE_PIECE_THRESHOLD && isMinimalist) {
            PlayGamesHelper.unlockAchievement(activity, R.string.achievement_purist)
        }
        if (!settings.playSounds) {
            PlayGamesHelper.unlockAchievement(activity, R.string.achievement_silent_solver)
        }
        if (activity.intent.getStringExtra(PhotoSource::class.simpleName)?.equals(PhotoSource.CAMERA.name) == true) {
            PlayGamesHelper.unlockAchievement(activity, R.string.achievement_photographer)
        }
        if (activity.intent.getStringExtra(PhotoSource::class.simpleName)?.equals(PhotoSource.GALLERY.name) == true) {
            PlayGamesHelper.unlockAchievement(activity, R.string.achievement_curator)
        }
        PlayGamesHelper.progressAchievement(activity, R.string.achievement_apprentice, ACHIEVEMENT_PROGRESS_STEP)
        PlayGamesHelper.progressAchievement(activity, R.string.achievement_journeyman, ACHIEVEMENT_PROGRESS_STEP)
        PlayGamesHelper.progressAchievement(activity, R.string.achievement_veteran, ACHIEVEMENT_PROGRESS_STEP)
        PlayGamesHelper.progressAchievement(activity, R.string.achievement_puzzle_master, ACHIEVEMENT_PROGRESS_STEP)
        AchievementHelper.checkRecordSetterAchievement(activity, elapsedTime, settings)
        AchievementHelper.checkCollectorAchievement(activity, settings)
        AchievementHelper.checkGalleryCompleteAchievement(activity, settings)

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour in 0..NIGHT_OWL_HOUR_END) {
            PlayGamesHelper.unlockAchievement(activity, R.string.achievement_night_owl)
        }

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
    fun updateAndShowHighScores(
        newTime: Int,
        difficultyKey: String,
        settings: Settings,
    ) {
        val highScores = settings.highscores.getOrPut(difficultyKey) { mutableListOf() }

        val currentScoreInSeconds = newTime
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val newScoreString =
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                currentScoreInSeconds / SECONDS_PER_MINUTE,
                currentScoreInSeconds % SECONDS_PER_MINUTE,
            ) +
                " - " + dateFormat.format(Date())

        highScores.add(newScoreString)

        // Sort by time (first part of the string)
        highScores.sortBy {
            val parts = it.split(" - ")
            val timeParts = parts[0].split(":")
            timeParts[0].toInt() * SECONDS_PER_MINUTE + timeParts[1].toInt()
        }
        while (highScores.size > MAX_HIGH_SCORES) {
            highScores.removeAt(MAX_HIGH_SCORES)
        }

        val indexOfNewScore = highScores.indexOf(newScoreString)
        showHighScorePopup(difficultyKey, highScores, indexOfNewScore)

        if (indexOfNewScore <= MAX_HIGH_SCORES && indexOfNewScore != -1) {
            Log.d(
                PuzzleCompletionFlow::class.simpleName,
                "New high score! indexOfNewScore: $indexOfNewScore, highScores.size: ${highScores.size}",
            )
            if (indexOfNewScore == 0 && highScores.size == MAX_HIGH_SCORES) {
                PlayGamesHelper.unlockAchievement(activity, R.string.achievement_top_of_the_charts)
            }
            FirebaseHelper.logEvent(activity, "new_highscore")
            Toast
                .makeText(activity, activity.getString(R.string.congratulations_top_10), Toast.LENGTH_LONG)
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
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.high_score_popup, null)

        val highScoreDifficulty = popupView.findViewById<TextView>(R.id.highScoreDifficulty)
        val highScoreListContainer =
            popupView.findViewById<LinearLayout>(R.id.highScoreListContainer)

        highScoreDifficulty.text = difficultyKey

        // Populate high scores
        for ((index, scoreString) in highScores.withIndex()) {
            val scoreTextView = TextView(activity)
            scoreTextView.setTextColor(activity.resources.getColor(R.color.white, null))
            scoreTextView.text = "${index + 1}. $scoreString"
            scoreTextView.textSize = HIGH_SCORE_TEXT_SIZE_SP
            if (index == newScoreIndex) {
                scoreTextView.setTypeface(null, Typeface.BOLD)
            }
            highScoreListContainer.addView(scoreTextView)
        }

        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setView(popupView)
        builder.setCancelable(false)

        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        alertDialog.show()

        popupView.findViewById<NeonButton>(R.id.highScoreOkButton).let {
            it.setOnClickListener {
                FirebaseHelper.logButtonClick(activity, "highscore_ok")
                alertDialog.dismiss()
            }
            it.setOnTouchListener { view, event ->
                NeonBtnOnPressChangeLook.applyPressedLook(view, event, activity)
                true
            }
        }
    }
}
