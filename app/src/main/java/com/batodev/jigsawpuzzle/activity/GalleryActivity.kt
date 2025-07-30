package com.batodev.jigsawpuzzle.activity

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.batodev.jigsawpuzzle.helpers.AdHelper
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.helpers.SettingsHelper
import com.github.chrisbanes.photoview.PhotoView
import java.io.File
import java.io.FileOutputStream

class GalleryActivity : AppCompatActivity() {
    private var images: MutableList<String> = mutableListOf()
    private var index: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.gallery_activity)

        val windowInsetsController = WindowCompat.getInsetsController(this.window, this.window.decorView)
        windowInsetsController.let { controller ->
            // Hide both bars
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // Sticky behavior - bars stay hidden until user swipes
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val settings = SettingsHelper.load(this)
        this.images = settings.uncoveredPics
        index = settings.lastSeenPic
        setImage(index)
        checkIfImageLeftRightButtonsShouldBeVisible()
    }

    private fun checkIfImageLeftRightButtonsShouldBeVisible() {
        if (index <= 0) {
            findViewById<Button>(R.id.gallery_left).visibility = View.GONE
        } else {
            findViewById<Button>(R.id.gallery_left).visibility = View.VISIBLE
        }
        if (index >= images.size - 1) {
            findViewById<Button>(R.id.gallery_right).visibility = View.GONE
        } else {
            findViewById<Button>(R.id.gallery_right).visibility = View.VISIBLE
        }
    }

    fun backClicked(view: View) {
        finish()
    }

    fun leftClicked(view: View) {
        if (index != 0) index--
        setImage(index)
        val settings = SettingsHelper.load(this)
        settings.lastSeenPic = index
        settings.addCounter++
        SettingsHelper.save(this, settings)
        AdHelper.showAdIfNeeded(this)
        checkIfImageLeftRightButtonsShouldBeVisible()
    }

    fun rightClicked(view: View) {
        if (index < images.size) index++
        setImage(index)
        val settings = SettingsHelper.load(this)
        settings.lastSeenPic = index
        settings.addCounter++
        SettingsHelper.save(this, settings)
        AdHelper.showAdIfNeeded(this)
        checkIfImageLeftRightButtonsShouldBeVisible()
    }

    private fun setImage(index: Int) {
        if (index >= 0 && index < images.size) {
            findViewById<PhotoView>(R.id.gallery_activity_background)
                .setImageBitmap(BitmapFactory.decodeStream(this.assets.open("img/${images[index]}")))
            this.index = index
        } else {
            setImage(index - 1)
        }
    }

    fun shareClicked(view: View) {
        val fileShared = copyToTempFile()
        val shareIntent = Intent(Intent.ACTION_SEND)
        val applicationId = this.application.applicationContext.packageName
        val uri = FileProvider.getUriForFile(this, "${applicationId}.fileprovider", fileShared)
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "image/*"
        ContextCompat.startActivity(this, shareIntent, null)
    }

    fun wallpaperClicked(view: View) {
        try {
            val fileShared = copyToTempFile()
            val wallpaperManager = WallpaperManager.getInstance(this)
            val bitmap = BitmapFactory.decodeFile(fileShared.absolutePath)
            wallpaperManager.setBitmap(bitmap)
            Toast.makeText(this, getString(R.string.wallpaper_ok), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Obsługa błędów
            Log.e(GalleryActivity::class.java.simpleName, "Błąd podczas ustawiania tapety", e)
            Toast.makeText(this, "Error: $e" , Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToTempFile(): File {
        val stream = this.assets.open("img/${images[index]}")
        val dirShared = File(filesDir, "shared")
        if (!dirShared.exists()) {
            dirShared.mkdir()
        }
        val fileShared = File(dirShared, "shared.jpg")
        if (fileShared.exists()) {
            fileShared.delete()
        }
        fileShared.createNewFile()
        FileOutputStream(fileShared).use {
            val buffer = ByteArray(10240)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                it.write(buffer, 0, bytesRead)
            }
            it.flush()
        }
        return fileShared
    }
}
