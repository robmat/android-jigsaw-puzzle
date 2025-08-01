package com.batodev.jigsawpuzzle.logic

interface PuzzleProgressListener {
    fun onProgressUpdate(progress: Int, max: Int)
    fun onCuttingFinished()
    fun postToHandler(r: Runnable)
}
