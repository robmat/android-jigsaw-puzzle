package com.batodev.jigsawpuzzle.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.helpers.AdHelper
import com.batodev.jigsawpuzzle.helpers.SettingsHelper

/**
 * The main menu activity of the application.
 */
class MainMenuActivity : Activity() {
    /**
     * Called when the activity is first created.
     * Initializes the UI, loads settings, and sets up event listeners for menu buttons.
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.main_menu_activity)

        val windowInsetsController = WindowCompat.getInsetsController(this.window, this.window.decorView)
        windowInsetsController.let { controller ->
            // Hide both bars
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // Sticky behavior - bars stay hidden until user swipes
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        SettingsHelper.load(this)
        AdHelper.loadAd(this)
        findViewById<Button>(R.id.main_menu_activity_play_the_game).setOnClickListener { play() }
        findViewById<Button>(R.id.main_menu_activity_unlocked_gallery).setOnClickListener { gallery() }
        findViewById<Button>(R.id.main_menu_activity_more_apps).setOnClickListener { moreApps() }
        findViewById<Button>(R.id.main_menu_activity_play_part_2).setOnClickListener { playPart2() }
        findViewById<ImageView>(R.id.main_menu_activity_emberfox_logo).setOnClickListener { moreApps() }
    }

    /**
     * Starts the {@link ImagePickActivity} to allow the user to select an image for the puzzle.
     * @see ImagePickActivity
     */
    fun play() {
        startActivity(Intent(this, ImagePickActivity::class.java))
    }

    /**
     * Opens the {@link GalleryActivity} if there are unlocked pictures, otherwise shows a toast message.
     * @see GalleryActivity
     * @see SettingsHelper
     */
    fun gallery() {
        SettingsHelper.load(this)
        if (!SettingsHelper.load(this).uncoveredPics.isEmpty()) {
            startActivity(Intent(this, GalleryActivity::class.java))
        } else {
            Toast.makeText(this, R.string.main_menu_activity_play_to_uncover, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the Google Play Store to show more applications from the developer.
     */
    fun moreApps() {
        startActivity(Intent(Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/dev?id=8228670503574649511".toUri()))
    }

    /**
     * Opens the Google Play Store to navigate to the second part of the game.
     */
    fun playPart2() {
        startActivity(Intent(Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=com.batodev.jigsawpuzzle3".toUri()))
    }
}
