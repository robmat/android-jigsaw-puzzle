package com.batodev.jigsawpuzzle.logic

import android.os.Handler
import android.os.Looper
import android.widget.TextView
import java.util.Locale

/**
 * A class for managing the stopwatch.
 * @param stopwatchText The {@link TextView} to display the elapsed time.
 */
class Stopwatch(
    private val stopwatchText: TextView,
) {
    companion object {
        private const val SECONDS_PER_HOUR = 3600
        private const val SECONDS_PER_MINUTE = 60
        private const val TICK_INTERVAL_MS = 1000L
    }

    var elapsedTime: Int = 0
    private val stopwatchHandler = Handler(Looper.getMainLooper())
    private lateinit var stopwatchRunnable: Runnable
    private var stopWatchRunning = false

    /**
     * Starts the stopwatch. The elapsed time will be updated every second.
     */
    fun start() {
        if (!stopWatchRunning) {
            stopWatchRunning = true
            stopwatchRunnable =
                Runnable {
                    elapsedTime++
                    val minutes = (elapsedTime % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
                    val seconds = elapsedTime % SECONDS_PER_MINUTE
                    stopwatchText.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                    stopwatchHandler.postDelayed(stopwatchRunnable, TICK_INTERVAL_MS)
                }
            stopwatchHandler.post(stopwatchRunnable)
        }
    }

    /**
     * Stops the stopwatch. The elapsed time will no longer be updated.
     */
    fun stop() {
        stopwatchHandler.removeCallbacks(stopwatchRunnable)
        stopWatchRunning = false
    }
}
