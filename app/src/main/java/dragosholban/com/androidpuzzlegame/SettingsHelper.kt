package dragosholban.com.androidpuzzlegame

import android.content.Context
import android.util.Log
import androidx.core.content.edit

private const val PREFS = "prefs"

private const val DISPLAY_ADD_EVERY_X_PIC_VIEW = "displayAddEveryXPicView"

private const val SEPARATOR = ";"

private const val ADD_COUNTER = "addCounter"

private const val LAST_SEEN_PIC = "lastSeenPic"

private const val UNCOVERED_PICS = "uncoveredPics"

private const val LAST_SET_DIFFICULTY = "lastSetDifficulty"

private const val LAST_SET_DIFFICULTY_CUSTOM_HEIGHT = "lastSetDifficultyCustomHeight"


private const val LAST_SET_DIFFICULTY_CUSTOM_WIDTH = "lastSetDifficultyCustomWidth"

object SettingsHelper {
    fun save(context: Context, settings: Settings) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(DISPLAY_ADD_EVERY_X_PIC_VIEW, settings.displayAddEveryXPicView)
            putInt(LAST_SET_DIFFICULTY_CUSTOM_HEIGHT, settings.lastSetDifficultyCustomHeight)
            putInt(LAST_SET_DIFFICULTY_CUSTOM_WIDTH, settings.lastSetDifficultyCustomWidth)
            putString(LAST_SET_DIFFICULTY, settings.lastSetDifficulty)
            putInt(ADD_COUNTER, settings.addCounter)
            putInt(LAST_SEEN_PIC, settings.lastSeenPic)
            putString(UNCOVERED_PICS, settings.uncoveredPics.joinToString(SEPARATOR))
            apply()
            Log.d(SettingsHelper.javaClass.simpleName, "Saved: $settings")
        }
    }

    fun load(context: Context) : Settings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val settings = Settings()
        settings.displayAddEveryXPicView = prefs.getInt(DISPLAY_ADD_EVERY_X_PIC_VIEW, 3)
        settings.lastSetDifficultyCustomHeight = prefs.getInt(LAST_SET_DIFFICULTY_CUSTOM_HEIGHT, 1)
        settings.lastSetDifficultyCustomWidth = prefs.getInt(LAST_SET_DIFFICULTY_CUSTOM_WIDTH, 0)
        settings.lastSetDifficulty = prefs.getString(LAST_SET_DIFFICULTY, ImagePickActivity.EASY)!!
        settings.addCounter = prefs.getInt(ADD_COUNTER, 0)
        settings.lastSeenPic = prefs.getInt(LAST_SEEN_PIC, -1)
        settings.uncoveredPics = prefs.getString(UNCOVERED_PICS, "")!!.split(SEPARATOR).toMutableList()
        settings.uncoveredPics.remove("")
        return settings
    }
}
