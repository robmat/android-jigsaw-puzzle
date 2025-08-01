package com.batodev.jigsawpuzzle.helpers

/**
 * A data class for storing the application settings.
 */
class Settings {
    var uncoveredPics: MutableList<String> = ArrayList()
    var lastSeenPic = 0
    var addCounter = 0
    var displayAddEveryXPicView = 2
    var showGridInBackgroundOfThePuzzle = true
    var lastSetDifficultyCustomHeight = 5
    var lastSetDifficultyCustomWidth = 3
    var showImageInBackgroundOfThePuzzle = true
    var playSounds = true
    var highscores: MutableMap<String, MutableList<String>> = mutableMapOf()
}
