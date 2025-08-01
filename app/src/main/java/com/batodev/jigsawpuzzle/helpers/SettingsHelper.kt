package com.batodev.jigsawpuzzle.helpers

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val PREFS = "prefs"
private const val DISPLAY_ADD_EVERY_X_PIC_VIEW = "displayAddEveryXPicView"
private const val SEPARATOR = ";"
private const val ADD_COUNTER = "addCounter"
private const val LAST_SEEN_PIC = "lastSeenPic"
private const val UNCOVERED_PICS = "uncoveredPics"
private const val SHOW_GRID_IN_BACKGROUND_OF_THE_PUZZLE = "showGridInBackgroundOfThePuzzle"
private const val LAST_SET_DIFFICULTY_CUSTOM_HEIGHT = "lastSetDifficultyCustomHeight"
private const val LAST_SET_DIFFICULTY_CUSTOM_WIDTH = "lastSetDifficultyCustomWidth"
private const val SHOW_IMAGE_IN_BACKGROUND_OF_THE_PUZZLE = "showImageInBackgroundOfThePuzzle"
private const val PLAY_SOUNDS = "playSounds"
private const val HIGHSCORES = "highscores"

/**
 * A helper object for saving and loading application settings.
 */
object SettingsHelper {
    private val gson = Gson()

    /**
     * Saves the provided {@link Settings} object to shared preferences.
     * @param context The {@link Context} used to access shared preferences.
     * @param settings The {@link Settings} object to be saved.
     */
    fun save(context: Context, settings: Settings) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(DISPLAY_ADD_EVERY_X_PIC_VIEW, settings.displayAddEveryXPicView)
            putInt(LAST_SET_DIFFICULTY_CUSTOM_HEIGHT, settings.lastSetDifficultyCustomHeight)
            putInt(LAST_SET_DIFFICULTY_CUSTOM_WIDTH, settings.lastSetDifficultyCustomWidth)
            putBoolean(SHOW_GRID_IN_BACKGROUND_OF_THE_PUZZLE, settings.showGridInBackgroundOfThePuzzle)
            putInt(ADD_COUNTER, settings.addCounter)
            putInt(LAST_SEEN_PIC, settings.lastSeenPic)
            putBoolean(SHOW_IMAGE_IN_BACKGROUND_OF_THE_PUZZLE, settings.showImageInBackgroundOfThePuzzle)
            putBoolean(PLAY_SOUNDS, settings.playSounds)
            putString(UNCOVERED_PICS, settings.uncoveredPics.joinToString(SEPARATOR))
            putString(HIGHSCORES, gson.toJson(settings.highscores))
            apply()
            Log.d(SettingsHelper.javaClass.simpleName, "Saved: $settings")
        }
    }

    /**
     * Loads the application settings from shared preferences.
     * If no settings are found, default values are used.
     * @param context The {@link Context} used to access shared preferences.
     * @return The loaded {@link Settings} object.
     */
    fun load(context: Context): Settings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val settings = Settings()
        settings.displayAddEveryXPicView = prefs.getInt(DISPLAY_ADD_EVERY_X_PIC_VIEW, 5)
        settings.lastSetDifficultyCustomHeight = prefs.getInt(LAST_SET_DIFFICULTY_CUSTOM_HEIGHT, 5)
        settings.lastSetDifficultyCustomWidth = prefs.getInt(LAST_SET_DIFFICULTY_CUSTOM_WIDTH, 3)
        settings.showGridInBackgroundOfThePuzzle = prefs.getBoolean(SHOW_GRID_IN_BACKGROUND_OF_THE_PUZZLE, true)
        settings.addCounter = prefs.getInt(ADD_COUNTER, 0)
        settings.lastSeenPic = prefs.getInt(LAST_SEEN_PIC, 0)
        settings.showImageInBackgroundOfThePuzzle = prefs.getBoolean(SHOW_IMAGE_IN_BACKGROUND_OF_THE_PUZZLE, true)
        settings.playSounds = prefs.getBoolean(PLAY_SOUNDS, true)
        settings.uncoveredPics = prefs.getString(UNCOVERED_PICS, "")!!.split(SEPARATOR).toMutableList()
        settings.uncoveredPics.remove("")
        val highscoresJson = prefs.getString(HIGHSCORES, "{}") ?: "{}"
        val type = object : TypeToken<MutableMap<String, MutableList<String>>>() {}.type
        settings.highscores = gson.fromJson(highscoresJson, type)
        return settings
    }
}
