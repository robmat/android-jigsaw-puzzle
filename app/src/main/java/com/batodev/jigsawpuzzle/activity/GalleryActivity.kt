package com.batodev.jigsawpuzzle.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.helpers.AdHelper
import com.batodev.jigsawpuzzle.helpers.SettingsHelper
import com.github.chrisbanes.photoview.PhotoView
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * An activity for displaying a gallery of unlocked images.
 */
class GalleryActivity : AppCompatActivity() {
    private var images: MutableList<String> = mutableListOf()
    private var index: Int = 0
    private var isAnimating: Boolean = false

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
        setContentView(R.layout.gallery_activity)

        val windowInsetsController =
            WindowCompat.getInsetsController(this.window, this.window.decorView)
        windowInsetsController.let { controller ->
            // Hide both bars
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // Sticky behavior - bars stay hidden until user swipes
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val settings = SettingsHelper.load(this)
        this.images = settings.uncoveredPics
        index = settings.lastSeenPic
        setImage(images[index])
        checkIfImageLeftRightButtonsShouldBeVisible()
        findViewById<ImageButton>(R.id.gallery_left).setOnClickListener { leftClicked() }
        findViewById<ImageButton>(R.id.gallery_right).setOnClickListener { rightClicked() }
        findViewById<ImageButton>(R.id.gallery_back_btn).setOnClickListener { backClicked() }
        findViewById<ImageButton>(R.id.gallery_share_btn).setOnClickListener { shareClicked() }
        findViewById<ImageButton>(R.id.gallery_wallpaper_btn).setOnClickListener { wallpaperClicked() }

        val photoView = findViewById<PhotoView>(R.id.gallery_activity_background)
        photoView.setOnSingleFlingListener { e1, e2, velocityX, velocityY ->
            flingHandling(e1, e2, velocityX, velocityY)
        }
    }

    private fun flingHandling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        Log.d(
            "GalleryActivity",
            "onFling: e1=$e1, e2=$e2, velocityX=$velocityX, velocityY=$velocityY"
        )
        val swipeThreshold = 200
        val swipeVelocityThreshold = 300
        val diffX = e2.x - e1.x
        return if (abs(diffX) > abs(e2.y - e1.y)) {
            if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                if (diffX > 0) {
                    leftClicked()
                } else {
                    rightClicked()
                }
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    /**
     * Checks if the left and right image navigation buttons should be visible based on the current image index.
     */
    private fun checkIfImageLeftRightButtonsShouldBeVisible() {
        findViewById<ImageButton>(R.id.gallery_left).visibility =
            if (index <= 0) View.GONE else View.VISIBLE
        findViewById<ImageButton>(R.id.gallery_right).visibility =
            if (index >= images.size - 1) View.GONE else View.VISIBLE
    }

    /**
     * Handles the click event for the back button, finishing the activity.
     */
    fun backClicked() {
        finish()
    }

    /**
     * Handles the click event for the left navigation button.
     * Decrements the image index, updates the displayed image, and saves settings.
     * Shows an ad if applicable.
     * @see SettingsHelper
     * @see AdHelper
     */
    fun leftClicked() {
        if (index > 0 && !isAnimating) {
            animateImageChange(-1)
            val settings = SettingsHelper.load(this)
            settings.lastSeenPic = index - 1
            settings.addCounter++
            SettingsHelper.save(this, settings)
            AdHelper.showAdIfNeeded(this)
        }
    }

    /**
     * Handles the click event for the right navigation button.
     * Increments the image index, updates the displayed image, and saves settings.
     * Shows an ad if applicable.
     * @see SettingsHelper
     * @see AdHelper
     */
    fun rightClicked() {
        if (index < images.size - 1 && !isAnimating) {
            animateImageChange(1)
            val settings = SettingsHelper.load(this)
            settings.lastSeenPic = index + 1
            settings.addCounter++
            SettingsHelper.save(this, settings)
            AdHelper.showAdIfNeeded(this)
        }
    }

    private fun animateImageChange(direction: Int) {
        isAnimating = true
        val photoView = findViewById<PhotoView>(R.id.gallery_activity_background)
        val duration = 200L

        val (pivotXOut, pivotXIn) = if (direction == 1) {
            Pair(0f, photoView.width.toFloat()) // Shrink to left, grow from right
        } else {
            Pair(photoView.width.toFloat(), 0f) // Shrink to right, grow from left
        }

        photoView.pivotX = pivotXOut
        photoView.pivotY = photoView.height / 2f

        val outSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(photoView, "scaleX", 1f, 0f),
                ObjectAnimator.ofFloat(photoView, "scaleY", 1f, 0f)
            )
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (direction == 1) {
                        index++
                    } else {
                        index--
                    }
                    setImage(images[index])
                    checkIfImageLeftRightButtonsShouldBeVisible()

                    photoView.pivotX = pivotXIn
                    photoView.pivotY = photoView.height / 2f

                    AnimatorSet().apply {
                        playTogether(
                            ObjectAnimator.ofFloat(photoView, "scaleX", 0f, 1f),
                            ObjectAnimator.ofFloat(photoView, "scaleY", 0f, 1f)
                        )
                        this.duration = duration
                        interpolator = AccelerateDecelerateInterpolator()
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                isAnimating = false
                            }
                        })
                        start()
                    }
                }
            })
        }
        outSet.start()
    }

    /**
     * Sets the image displayed in the gallery.
     * @param index The index of the image to display from the {@link #images} list.
     */
    private fun setImage(imageName: String) {
        if (index >= 0 && index < images.size) {
            try {
                this.assets.open("img/$imageName")
                    .use { inputStream -> // .use will auto-close the stream
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            findViewById<PhotoView>(R.id.gallery_activity_background)
                                .setImageBitmap(bitmap)
                        } else {
                            Log.w(
                                "GalleryActivity",
                                "Failed to decode bitmap for image: img/${images[index]}"
                            )
                        }
                    }
            } catch (e: java.io.IOException) {
                Log.w("GalleryActivity", "Error opening image: img/${images[index]}", e)
            }
        }
    }

    /**
     * Handles the click event for the share button.
     * Copies the current image to a temporary file and shares it using an Intent.
     * @see FileProvider
     */
    fun shareClicked() {
        try {
            val fileShared = copyToTempFile()
            val shareIntent = Intent(Intent.ACTION_SEND)
            val applicationId = this.application.applicationContext.packageName
            val uri = FileProvider.getUriForFile(this, "${applicationId}.fileprovider", fileShared)
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/*"
            ContextCompat.startActivity(this, shareIntent, null)
        } catch (e: Exception) {
            Log.w(GalleryActivity::class.java.simpleName, "Error setting wallpaper", e)
            Toast.makeText(this, "Error: $e", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handles the click event for the wallpaper button.
     * Sets the current image as the device's wallpaper.
     * @throws Exception if there is an error setting the wallpaper.
     */
    fun wallpaperClicked() {
        try {
            val fileShared = copyToTempFile()
            val wallpaperManager = WallpaperManager.getInstance(this)
            val bitmap = BitmapFactory.decodeFile(fileShared.absolutePath)
            wallpaperManager.setBitmap(bitmap)
            Toast.makeText(this, getString(R.string.wallpaper_ok), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.w(GalleryActivity::class.java.simpleName, "Error setting wallpaper", e)
            Toast.makeText(this, "Error: $e", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies the currently displayed image to a temporary file.
     * @return The temporary File object.
     * @throws java.io.IOException if an I/O error occurs during file operations.
     */
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
            while (stream.read(buffer).also { bytes -> bytesRead = bytes } != -1) {
                it.write(buffer, 0, bytesRead)
            }
            it.flush()
        }
        return fileShared
    }
}
