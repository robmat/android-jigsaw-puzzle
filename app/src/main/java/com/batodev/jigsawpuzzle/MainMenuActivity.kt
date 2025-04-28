package com.batodev.jigsawpuzzle

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainMenuActivity : Activity() {
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
    }

    fun play(view: View) {
        startActivity(Intent(this, ImagePickActivity::class.java))
    }
    fun gallery(view: View) {
        SettingsHelper.load(this)
        if (!SettingsHelper.load(this).uncoveredPics.isEmpty()) {
            startActivity(Intent(this, GalleryActivity::class.java))
        } else {
            Toast.makeText(this, R.string.main_menu_activity_play_to_uncover, Toast.LENGTH_SHORT).show()
        }
    }
    fun moreApps(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/dev?id=8228670503574649511".toUri()))
    }
}
