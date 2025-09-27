package com.batodev.jigsawpuzzle.helpers

import android.app.Activity
import android.content.Context
import android.util.Log
import com.batodev.jigsawpuzzle.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AchievementHelper {
    fun updateDailyRitualAchievement(context: Context) {
        val TAG = AchievementHelper::class.java.simpleName
        val settings = SettingsHelper.load(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Log.v(TAG, "Today's date: $today")

        if (settings.dailyRitualPlayDates.isEmpty()) {
            Log.v(TAG, "No previous play dates. Adding today.")
            settings.dailyRitualPlayDates.add(today)
        } else {
            val lastPlayDate = settings.dailyRitualPlayDates.last()
            Log.v(TAG, "Last play date: $lastPlayDate")
            if (today != lastPlayDate) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val lastDate = sdf.parse(lastPlayDate)
                val calendar = Calendar.getInstance()
                calendar.time = lastDate!!
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val nextDay = sdf.format(calendar.time)
                Log.v(TAG, "Next expected play date: $nextDay")
                if (today == nextDay) {
                    Log.v(TAG, "Played on consecutive day. Adding today.")
                    settings.dailyRitualPlayDates.add(today)
                } else {
                    Log.v(TAG, "Streak broken. Resetting play dates.")
                    settings.dailyRitualPlayDates.clear()
                    settings.dailyRitualPlayDates.add(today)
                }
            } else {
                Log.v(TAG, "Already played today. No action taken.")
            }
        }

        val currentStreak = settings.dailyRitualPlayDates.size
        Log.v(TAG, "Current streak: $currentStreak")
        if (currentStreak > settings.maxDailyRitualStreak) {
            Log.v(TAG, "New max streak: $currentStreak")
            settings.maxDailyRitualStreak = currentStreak
            if (context is Activity) {
                Log.v(TAG, "Reporting achievement progress.")
                PlayGamesHelper.progressAchievement(context, R.string.achievement_daily_ritual, 1)
            }
        }

        SettingsHelper.save(context, settings)
        Log.v(TAG, "Settings saved.")
    }

    fun checkRecordSetterAchievement(context: Activity, elapsedTime: Int, settings: Settings) {
        val difficultyKey =
            "${settings.lastSetDifficultyCustomWidth}x${settings.lastSetDifficultyCustomHeight}"
        val highScores = settings.highscores[difficultyKey]
        if (highScores != null && highScores.isNotEmpty()) {
            val bestTime = highScores[0]
            val parts = bestTime.split(" - ")
            val timeParts = parts[0].split(":")
            val bestTimeInSeconds = timeParts[0].toInt() * 60 + timeParts[1].toInt()
            if (elapsedTime < bestTimeInSeconds) {
                PlayGamesHelper.unlockAchievement(context, R.string.achievement_record_setter)
            }
        }
    }

}

