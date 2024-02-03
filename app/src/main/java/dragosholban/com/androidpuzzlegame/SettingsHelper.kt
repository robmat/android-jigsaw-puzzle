package dragosholban.com.androidpuzzlegame

import android.app.Activity
import android.content.Context
import androidx.core.content.edit

private const val PREFS = "prefs"

private const val DISPLAY_ADD_EVERY_X_PIC_VIEW = "displayAddEveryXPicView"

private const val SEPARATOR = ";"

private const val ADD_COUNTER = "addCounter"

private const val LAST_SEEN_PIC = "lastSeenPic"

private const val UNCOVERED_PICS = "uncoveredPics"

object SettingsHelper {
    fun save(context: Context, settings: Settings) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(DISPLAY_ADD_EVERY_X_PIC_VIEW, settings.displayAddEveryXPicView)
            putInt(ADD_COUNTER, settings.addCounter)
            putInt(LAST_SEEN_PIC, settings.lastSeenPic)
            putString(UNCOVERED_PICS, settings.uncoveredPics.joinToString(SEPARATOR))
            apply()
        }
    }

    fun load(context: Context) : Settings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val settings = Settings()
        settings.displayAddEveryXPicView = prefs.getInt(DISPLAY_ADD_EVERY_X_PIC_VIEW, 3)
        settings.addCounter = prefs.getInt(ADD_COUNTER, 0)
        settings.lastSeenPic = prefs.getInt(LAST_SEEN_PIC, -1)
        settings.uncoveredPics = prefs.getString(UNCOVERED_PICS, "")!!.split(SEPARATOR).toMutableList()
        settings.uncoveredPics.remove("")
        return settings
    }
}
