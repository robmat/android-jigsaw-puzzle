package com.batodev.jigsawpuzzle.helpers

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity

/**
 * A helper object for playing sounds.
 */
object SoundsPlayer {
    /**
     * Plays a sound from the specified resource ID.
     * The sound will only play if the `playSounds` setting is enabled.
     * @param res The resource ID of the sound to play (e.g., `R.raw.sound_file`).
     * @param activity The {@link AppCompatActivity} context used to create the {@link MediaPlayer}.
     * @see SettingsHelper
     */
    fun play(res : Int, activity: AppCompatActivity) {
        if (SettingsHelper.load(activity).playSounds) {
            val mp = MediaPlayer.create(activity, res)
            mp.setOnCompletionListener { mp.release() }
            mp.start()
        }
    }
}
