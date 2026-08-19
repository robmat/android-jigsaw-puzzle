package com.batodev.jigsawpuzzle.activity

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ActivityNotFoundException
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
import com.google.android.material.snackbar.Snackbar
import com.smb.glowbutton.NeonButton
import java.io.File

/**
 * The main menu activity of the application.
 */
class MainMenuActivity : AppCompatActivity() {
    private lateinit var menuUi: MenuUi
    private lateinit var achievementsFlow: AchievementsFlow

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
        menuUi = MenuUi(this)
        // Registering the ActivityResultLauncher must happen before the activity is STARTED,
        // so this can't be deferred to showAchievements()'s first call.
        achievementsFlow = AchievementsFlow(this)
    }

    override fun onResume() {
        super.onResume()
        menuUi.onResume()
    }

    override fun onPause() {
        super.onPause()
        menuUi.onPause()
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
            Snackbar.make(
                findViewById(android.R.id.content),
                R.string.main_menu_activity_play_to_uncover,
                Snackbar.LENGTH_SHORT
            )
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

    /**
     * Shows the Play Games achievements screen, signing the player in first if needed.
     */
    fun showAchievements() {
        achievementsFlow.show()
    }
}

private class MenuButtons(
    val playButton: NeonButton,
    val continueButton: NeonButton,
    val galleryButton: NeonButton,
    val moreAppsButton: NeonButton,
    val playPart2Button: NeonButton,
)

private class MenuDecor(
    val emberfoxLogo: ImageView,
    val achievementButton: ImageView,
)

private data class MenuScreen(val buttons: MenuButtons, val decor: MenuDecor)

/**
 * Owns the main-menu screen's fade-in/stagger animations and the "continue game"
 * save-state broadcast receivers, split out of [MainMenuActivity] so that class
 * only holds the button click handlers themselves.
 */
private class MenuUi(private val activity: MainMenuActivity) {
    companion object {
        private const val BACKGROUND_FADE_ALPHA = 0.4f
        private const val BACKGROUND_FADE_DURATION_MS = 2000L
        private const val MENU_ANIMATION_DELAY_MS = 500L
        private const val MENU_BUTTON_INITIAL_SCALE = 0.5f
        private const val MENU_BUTTON_ANIMATION_DURATION_MS = 500L
        private const val MENU_BUTTON_STAGGER_DELAY_MS = 200L
    }

    private val saveStartedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            activity.findViewById<NeonButton>(R.id.main_menu_activity_continue_game).visibility = View.GONE
        }
    }

    private val saveCompleteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val continueButton = activity.findViewById<NeonButton>(R.id.main_menu_activity_continue_game)
            if (checkIfSaveIsAvailable() && continueButton.visibility != View.VISIBLE) {
                animateMenuButtons(continueButton)
            }
        }
    }

    fun onResume() {
        fadeInBackground()
        registerSaveReceivers()
        val screen = initMenuButtons()
        scheduleMenuAnimation(screen)
    }

    fun onPause() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(saveStartedReceiver)
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(saveCompleteReceiver)
    }

    private fun fadeInBackground() {
        val background = activity.findViewById<ImageView>(R.id.main_menu_background)
        ObjectAnimator.ofFloat(background, "alpha", 0f, BACKGROUND_FADE_ALPHA).apply {
            duration = BACKGROUND_FADE_DURATION_MS
            start()
        }
    }

    private fun registerSaveReceivers() {
        LocalBroadcastManager.getInstance(activity).registerReceiver(
            saveStartedReceiver,
            IntentFilter("com.batodev.jigsawpuzzle.SAVE_STARTED")
        )
        LocalBroadcastManager.getInstance(activity).registerReceiver(
            saveCompleteReceiver,
            IntentFilter("com.batodev.jigsawpuzzle.SAVE_COMPLETE")
        )
    }

    private fun initMenuButtons(): MenuScreen {
        val buttons = MenuButtons(
            playButton = activity.findViewById(R.id.main_menu_activity_play_the_game),
            continueButton = activity.findViewById(R.id.main_menu_activity_continue_game),
            galleryButton = activity.findViewById(R.id.main_menu_activity_unlocked_gallery),
            moreAppsButton = activity.findViewById(R.id.main_menu_activity_more_apps),
            playPart2Button = activity.findViewById(R.id.main_menu_activity_play_part_2),
        )
        val decor = MenuDecor(
            emberfoxLogo = activity.findViewById(R.id.main_menu_activity_emberfox_logo),
            achievementButton = activity.findViewById(R.id.main_menu_activity_achievements),
        )

        val allViews = listOf(
            buttons.playButton,
            buttons.continueButton,
            buttons.galleryButton,
            buttons.moreAppsButton,
            buttons.playPart2Button,
            decor.emberfoxLogo,
            decor.achievementButton
        )
        for (view in allViews) {
            view.visibility = View.INVISIBLE
        }

        buttons.playButton.setOnClickListener { activity.play() }
        buttons.continueButton.setOnClickListener { activity.continueGame() }
        buttons.galleryButton.setOnClickListener { activity.gallery() }
        buttons.moreAppsButton.setOnClickListener { activity.moreApps() }
        buttons.playPart2Button.setOnClickListener { activity.playPart2() }
        decor.achievementButton.setOnClickListener { activity.showAchievements() }

        NeonBtnOnPressChangeLook.setupNeonButtonTouchListeners(
            activity,
            buttons.playButton,
            buttons.continueButton,
            buttons.galleryButton,
            buttons.moreAppsButton,
            buttons.playPart2Button
        )
        return MenuScreen(buttons, decor)
    }

    private fun scheduleMenuAnimation(screen: MenuScreen) {
        val (buttons, decor) = screen
        // Delay the menu button animations
        Handler(Looper.getMainLooper()).postDelayed({
            val isSaving =
                PuzzleActivity.Companion.puzzleStatus.get() == PuzzleActivity.Companion.PuzzleStatus.SAVING
            val saveExists = checkIfSaveIsAvailable()

            if (saveExists && !isSaving) {
                animateMenuButtons(
                    buttons.playButton,
                    buttons.continueButton,
                    buttons.galleryButton,
                    buttons.moreAppsButton,
                    buttons.playPart2Button,
                    decor.emberfoxLogo,
                    decor.achievementButton
                )
            } else {
                animateMenuButtons(
                    buttons.playButton,
                    buttons.galleryButton,
                    buttons.moreAppsButton,
                    buttons.playPart2Button,
                    decor.emberfoxLogo,
                    decor.achievementButton
                )
                buttons.continueButton.visibility = View.GONE
            }
        }, MENU_ANIMATION_DELAY_MS)
    }

    private fun animateMenuButtons(vararg views: View) {
        for ((index, view) in views.withIndex()) {
            // Make view visible just before animation starts
            view.visibility = View.VISIBLE

            view.alpha = 0f
            view.scaleX = MENU_BUTTON_INITIAL_SCALE
            view.scaleY = MENU_BUTTON_INITIAL_SCALE

            val animator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(view, "scaleX", MENU_BUTTON_INITIAL_SCALE, 1f),
                    ObjectAnimator.ofFloat(view, "scaleY", MENU_BUTTON_INITIAL_SCALE, 1f)
                )
                duration = MENU_BUTTON_ANIMATION_DURATION_MS
                interpolator = AccelerateDecelerateInterpolator()
                startDelay = index * MENU_BUTTON_STAGGER_DELAY_MS
            }
            animator.start()
        }
    }

    private fun checkIfSaveIsAvailable(): Boolean {
        val savedGameFile = File(activity.filesDir, "saved_game/gamestate.json")
        Log.d(
            MenuUi::class.simpleName,
            "savedGameFile.exists(): ${savedGameFile.exists()}"
        )
        return savedGameFile.exists()
    }
}

/**
 * Owns the Play Games sign-in + achievements-screen launch flow, split out of
 * [MainMenuActivity] so that class only holds the button click handlers themselves.
 */
private class AchievementsFlow(private val activity: AppCompatActivity) {
    private val launcher: ActivityResultLauncher<Intent> = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(AchievementsFlow::class.simpleName, "Returned from Achievements ${result.resultCode}")
    }

    fun show() {
        signInSilently {
            PlayGames.getAchievementsClient(activity)
                .achievementsIntent
                .addOnSuccessListener { intent -> launchAchievements(intent) }
                .addOnFailureListener { e ->
                    Log.e(AchievementsFlow::class.simpleName, "Couldn't get Achievements Intent", e)
                }
        }
    }

    private fun launchAchievements(intent: Intent) {
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(AchievementsFlow::class.simpleName, "Could not launch achievements intent", e)
        }
    }

    private fun signInSilently(onSuccess: () -> Unit) {
        val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask ->
            if (isAuthenticatedTask.isSuccessful && isAuthenticatedTask.result.isAuthenticated) {
                // User is already signed in or silent sign-in was successful
                Log.d(AchievementsFlow::class.simpleName, "User is authenticated.")
                onSuccess.invoke()
            } else {
                // User is not signed in or silent sign-in failed
                Log.d(
                    AchievementsFlow::class.simpleName,
                    "User not authenticated. Attempting interactive sign-in."
                )
                signInInteractively(onSuccess)
            }
        }
    }

    private fun signInInteractively(onSuccess: () -> Unit) {
        val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
        gamesSignInClient.signIn().addOnCompleteListener { signInTask ->
            if (signInTask.isSuccessful && signInTask.result.isAuthenticated) {
                Log.d(AchievementsFlow::class.simpleName, "Interactive sign-in successful.")
                onSuccess()
            } else {
                Log.e(
                    AchievementsFlow::class.simpleName,
                    "Interactive sign-in failed ${signInTask.result} ${signInTask.exception}.",
                    signInTask.exception
                )
            }
        }
    }
}
