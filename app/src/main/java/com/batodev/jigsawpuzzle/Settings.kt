package com.batodev.jigsawpuzzle

class Settings {
    var uncoveredPics: MutableList<String> = ArrayList()
    var lastSeenPic = 0
    var addCounter = 0
    var displayAddEveryXPicView = 2
    var lastSetDifficulty = "Easy"
    var lastSetDifficultyCustomHeight = 3
    var lastSetDifficultyCustomWidth = 3
    override fun toString(): String {
        return "Settings(uncoveredPics=$uncoveredPics, lastSeenPic=$lastSeenPic, addCounter=$addCounter, displayAddEveryXPicView=$displayAddEveryXPicView, lastSetDifficulty='$lastSetDifficulty', lastSetDifficultyCustomHeight=$lastSetDifficultyCustomHeight, lastSetDifficultyCustomWidth=$lastSetDifficultyCustomWidth)"
    }
}
