package com.batodev.jigsawpuzzle.activity

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.helpers.AdHelper
import com.batodev.jigsawpuzzle.helpers.FirebaseHelper
import com.batodev.jigsawpuzzle.helpers.NeonBtnOnPressChangeLook
import com.batodev.jigsawpuzzle.helpers.PlayGamesHelper
import com.batodev.jigsawpuzzle.helpers.RemoveBars
import com.batodev.jigsawpuzzle.helpers.SettingsHelper
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.smb.glowbutton.NeonButton
import java.io.File

/**
 * The main menu activity of the application.
 */
class MainMenuActivity : AppCompatActivity() {
    private lateinit var achievementsLauncher: ActivityResultLauncher<Intent>

    /**
     * Called when the activity is first created.
     * Initializes the UI, loads settings, and sets up event listeners for menu buttons.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_menu_activity)
        FirebaseHelper.logScreenView(this, "MainMenuActivity")
        RemoveBars.removeTopBottomAndActionBars(this)
        SettingsHelper.load(this)
        AdHelper.loadAd(this)
        PlayGamesSdk.initialize(this)
        initializeAchievementsLauncher()
    }

    private val saveStartedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            findViewById<NeonButton>(R.id.main_menu_activity_continue_game).visibility = View.GONE
        }
    }

    private val saveCompleteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val continueButton = findViewById<NeonButton>(R.id.main_menu_activity_continue_game)
            if (checkIfSaveIsAvailable() && continueButton.visibility != View.VISIBLE) {
                animateMenuButtons(continueButton)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val background = findViewById<ImageView>(R.id.main_menu_background)
        ObjectAnimator.ofFloat(background, "alpha", 0f, 0.4f).apply {
            duration = 2000
            start()
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            saveStartedReceiver,
            IntentFilter("com.batodev.jigsawpuzzle.SAVE_STARTED")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            saveCompleteReceiver,
            IntentFilter("com.batodev.jigsawpuzzle.SAVE_COMPLETE")
        )
        val playButton = findViewById<NeonButton>(R.id.main_menu_activity_play_the_game)
        val continueButton = findViewById<NeonButton>(R.id.main_menu_activity_continue_game)
        val galleryButton = findViewById<NeonButton>(R.id.main_menu_activity_unlocked_gallery)
        val moreAppsButton = findViewById<NeonButton>(R.id.main_menu_activity_more_apps)
        val playPart2Button = findViewById<NeonButton>(R.id.main_menu_activity_play_part_2)
        val emberfoxLogo = findViewById<ImageView>(R.id.main_menu_activity_emberfox_logo)
        val achievementButton = findViewById<ImageView>(R.id.main_menu_activity_achievements)

        playButton.visibility = View.INVISIBLE
        continueButton.visibility = View.INVISIBLE
        galleryButton.visibility = View.INVISIBLE
        moreAppsButton.visibility = View.INVISIBLE
        playPart2Button.visibility = View.INVISIBLE
        emberfoxLogo.visibility = View.INVISIBLE
        achievementButton.visibility = View.INVISIBLE

        playButton.setOnClickListener { play() }
        continueButton.setOnClickListener { continueGame() }
        galleryButton.setOnClickListener { gallery() }
        moreAppsButton.setOnClickListener { moreApps() }
        playPart2Button.setOnClickListener { playPart2() }
        achievementButton.setOnClickListener { showAchievements() }

        NeonBtnOnPressChangeLook.setupNeonButtonTouchListeners(
            this,
            playButton,
            continueButton,
            galleryButton,
            moreAppsButton,
            playPart2Button
        )

        // Delay the menu button animations
        Handler(Looper.getMainLooper()).postDelayed({
            val isSaving =
                PuzzleActivity.Companion.puzzleStatus.get() == PuzzleActivity.Companion.PuzzleStatus.SAVING
            val saveExists = checkIfSaveIsAvailable()

            if (saveExists && !isSaving) {
                animateMenuButtons(
                    playButton,
                    continueButton,
                    galleryButton,
                    moreAppsButton,
                    playPart2Button,
                    emberfoxLogo,
                    achievementButton
                )
            } else {
                animateMenuButtons(
                    playButton,
                    galleryButton,
                    moreAppsButton,
                    playPart2Button,
                    emberfoxLogo,
                    achievementButton
                )
                continueButton.visibility = View.GONE
            }
        }, 500)

    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(saveStartedReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(saveCompleteReceiver)
    }

    private fun signInSilently(onSuccess: () -> Unit = {}) {
        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask ->
            if (isAuthenticatedTask.isSuccessful && isAuthenticatedTask.result.isAuthenticated) {
                // User is already signed in or silent sign-in was successful
                Log.d(MainMenuActivity::class.simpleName, "User is authenticated.")
                // Now you can proceed to show achievements
                onSuccess.invoke()
            } else {
                // User is not signed in or silent sign-in failed
                Log.d(
                    MainMenuActivity::class.simpleName,
                    "User not authenticated. Attempting interactive sign-in."
                )
                signInInteractively(onSuccess)
            }
        }
    }

    private fun signInInteractively(onSuccess: () -> Unit = {}) {
        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.signIn().addOnCompleteListener { signInTask ->
            if (signInTask.isSuccessful && signInTask.result.isAuthenticated) {
                Log.d(MainMenuActivity::class.simpleName, "Interactive sign-in successful.")
                onSuccess()
            } else {
                Log.e(
                    MainMenuActivity::class.simpleName,
                    "Interactive sign-in failed ${signInTask.result} ${signInTask.exception}.",
                    signInTask.exception
                )
            }
        }
    }

    private fun initializeAchievementsLauncher() {
        achievementsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(
                MainMenuActivity::class.simpleName,
                "Returned from Achievements ${result.resultCode}"
            )
        }
    }

    private fun animateMenuButtons(vararg views: View) {
        for ((index, view) in views.withIndex()) {
            // Make view visible just before animation starts
            view.visibility = View.VISIBLE

            view.alpha = 0f
            view.scaleX = 0.5f
            view.scaleY = 0.5f

            val animator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1f),
                    ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1f)
                )
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
                startDelay = (index * 200).toLong()
            }
            animator.start()
        }
    }

    /**
     * Starts the {@link ImagePickActivity} to allow the user to select an image for the puzzle.
     * @see ImagePickActivity
     */
    fun play() {
        FirebaseHelper.logButtonClick(this, "play")
        startActivity(Intent(this, ImagePickActivity::class.java))
    }

    /**
     * Starts the {@link PuzzleActivity} to continue the saved game.
     * @see PuzzleActivity
     */
    fun continueGame() {
        FirebaseHelper.logButtonClick(this, "continue_game")
        startActivity(Intent(this, PuzzleActivity::class.java))
    }

    /**
     * Opens the {@link GalleryActivity} if there are unlocked pictures, otherwise shows a toast message.
     * @see GalleryActivity
     * @see SettingsHelper
     */
    fun gallery() {
        FirebaseHelper.logButtonClick(this, "gallery")
        SettingsHelper.load(this)
        if (!SettingsHelper.load(this).uncoveredPics.isEmpty()) {
            startActivity(Intent(this, GalleryActivity::class.java))
        } else {
            Toast.makeText(this, R.string.main_menu_activity_play_to_uncover, Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Opens the Google Play Store to show more applications from the developer.
     */
    fun moreApps() {
        FirebaseHelper.logButtonClick(this, "more_apps")
        PlayGamesHelper.unlockAchievement(this, R.string.achievement_window_shopper)
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/dev?id=8228670503574649511".toUri()
            )
        )
    }

    /**
     * Opens the Google Play Store to navigate to the second part of the game.
     */
    fun playPart2() {
        FirebaseHelper.logButtonClick(this, "play_part_2")
        PlayGamesHelper.unlockAchievement(this, R.string.achievement_to_be_continued___)
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=com.batodev.jigsawpuzzle3".toUri()
            )
        )
    }

    private fun checkIfSaveIsAvailable(): Boolean {
        val savedGameFile = File(filesDir, "saved_game/gamestate.json")
        Log.d(
            MainMenuActivity::class.simpleName,
            "savedGameFile.exists(): ${savedGameFile.exists()}"
        )
        return savedGameFile.exists()
    }

    private fun showAchievements() {
        signInSilently() {
            PlayGames.getAchievementsClient(this)
                .achievementsIntent
                .addOnSuccessListener { intent ->
                    try {
                        achievementsLauncher.launch(intent)
                    } catch (e: Exception) {
                        Log.e(
                            MainMenuActivity::class.simpleName,
                            "Could not launch achievements intent",
                            e
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(MainMenuActivity::class.simpleName, "Couldn't get Achievements Intent", e)
                }
        }
    }
}
