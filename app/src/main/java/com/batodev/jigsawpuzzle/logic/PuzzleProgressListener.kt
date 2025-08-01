package com.batodev.jigsawpuzzle.logic

/**
 * An interface for listening to puzzle progress events.
 */
interface PuzzleProgressListener {
    /**
     * Called to report updates on the puzzle cutting progress.
     * @param progress The current progress value.
     * @param max The maximum progress value.
     */
    fun onProgressUpdate(progress: Int, max: Int)
    /**
     * Called when the puzzle cutting process has completed.
     */
    fun onCuttingFinished()
    /**
     * Posts a {@link Runnable} to be executed on the main thread.
     * @param r The {@link Runnable} to be executed.
     */
    fun postToHandler(r: Runnable)
}
