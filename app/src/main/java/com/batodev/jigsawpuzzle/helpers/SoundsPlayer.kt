package com.batodev.jigsawpuzzle.helpers

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity

/**
 * A helper object for playing sounds.
 */
object SoundsPlayer {
    fun play(res : Int, activity: AppCompatActivity) {
        if (SettingsHelper.load(activity).playSounds) {
            val mp = MediaPlayer.create(activity, res)
            mp.setOnCompletionListener { mp.release() }
            mp.start()
        }
    }
}
