package dragosholban.com.androidpuzzlegame

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageButton
import com.batodev.picsofprettygirls9.activities.SettingsHelper
import com.github.chrisbanes.photoview.PhotoView

class GalleryActivity : Activity() {
    private var images: MutableList<String>? = null
    private var index: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.gallery_activity)
        this.images = SettingsHelper.load(this).uncoveredPics
        SettingsHelper.load(this)
        index = SettingsHelper.load(this).lastSeenPic
        setImage(index)
        findViewById<ImageButton>(R.id.gallery_back_btn).isActivated = true
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
    }

    fun rightClicked(view: View) {
        if (index < images!!.size) index++
        setImage(index)
        val settings = SettingsHelper.load(this)
        settings.lastSeenPic = index
        settings.addCounter++
        SettingsHelper.save(this, settings)
        AdHelper.showAdIfNeeded(this)
    }

    private fun setImage(index: Int) {
        if (index >= 0 && index < images!!.size) {
            findViewById<PhotoView>(R.id.gallery_activity_background)
                .setImageBitmap(BitmapFactory.decodeStream(this.assets.open("memo-images/${images!![index]}")))
            this.index = index
        } else {
            setImage(index - 1)
        }
    }
}