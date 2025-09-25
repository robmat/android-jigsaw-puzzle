package com.batodev.jigsawpuzzle.helpers

import android.app.Activity
import android.content.Context
import com.batodev.jigsawpuzzle.R
import com.google.android.gms.games.PlayGames

object PlayGamesHelper {

    fun unlockAchievement(context: Activity, achievementId: Int) {
        PlayGames.getAchievementsClient(context)
            .unlock(context.getString(achievementId))
    }
}
