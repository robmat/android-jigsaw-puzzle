package dragosholban.com.androidpuzzlegame

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.Toast
import com.batodev.picsofprettygirls9.activities.SettingsHelper

class MainMenuActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.main_menu_activity)
        SettingsHelper.load(this)
        findViewById<Button>(R.id.main_menu_activity_play_the_game).isActivated = true
        findViewById<Button>(R.id.main_menu_activity_unlocked_gallery).isActivated = true
        findViewById<Button>(R.id.main_menu_activity_more_apps).isActivated = true
    }

    fun play(view: View) {
        startActivity(Intent(this, MainActivity::class.java))
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
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=8228670503574649511")))
    }
}