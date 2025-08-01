package com.batodev.jigsawpuzzle.logic

/**
 * An interface for listening to puzzle progress events.
 */
interface PuzzleProgressListener {
    fun onProgressUpdate(progress: Int, max: Int)
    fun onCuttingFinished()
    fun postToHandler(r: Runnable)
}
