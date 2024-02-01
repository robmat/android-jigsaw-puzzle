package com.batodev.picsofprettygirls9.activities

import android.app.Activity
import dragosholban.com.androidpuzzlegame.Settings

const val SETTINGS_FILE_NAME: String = "settings.xml"

object SettingsHelper {
    fun save(activity: Activity, settings: Settings) {

    }

    fun load(activity: Activity) : Settings {
        return Settings()
    }
}
