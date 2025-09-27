package com.batodev.jigsawpuzzle.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RelativeLayout
import com.batodev.jigsawpuzzle.helpers.FirebaseHelper
import com.batodev.jigsawpuzzle.helpers.PlayGamesHelper
import com.batodev.jigsawpuzzle.helpers.AchievementHelper
import com.batodev.jigsawpuzzle.logic.PuzzleGameManager
import com.batodev.jigsawpuzzle.R
import com.otaliastudios.zoom.ZoomLayout
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A touch listener for puzzle pieces.
 * Handles touch events to allow users to drag and drop puzzle pieces.
 * @param puzzleGameManager The {@link PuzzleGameManager} instance to interact with game logic.
 * @param zoomableLayout The {@link ZoomLayout} that contains the puzzle pieces, used for coordinate transformation.
 */
class TouchListener(
    private val puzzleGameManager: PuzzleGameManager,
    private val zoomableLayout: ZoomLayout
) : OnTouchListener {
    private var xDelta = 0f
    private var yDelta = 0f

    /**
     * Called when a touch event is dispatched to a view.
     * Handles dragging of puzzle pieces and checks for game over conditions when a piece is placed.
     * @param view The {@link View} (puzzle piece) that received the touch event.
     * @param motionEvent The {@link MotionEvent} object containing full information about the event.
     * @return True if the listener has consumed the event, false otherwise.
     * @see PuzzlePiece
     * @see PuzzleGameManager#checkGameOver()
     */
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val x = motionEvent.rawX / zoomableLayout.zoom
        val y = motionEvent.rawY / zoomableLayout.zoom
        val tolerance = sqrt(
            view.width.toDouble().pow(2.0) + view.height.toDouble().pow(2.0)
        ) / 10
        val piece = view as PuzzlePiece
        if (!piece.canMove) {
            return true
        }
        val lParams = view.layoutParams as RelativeLayout.LayoutParams
        when (motionEvent.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                xDelta = x - lParams.leftMargin
                yDelta = y - lParams.topMargin
                Log.v(TouchListener::class.simpleName, "ACTION_DOWN: x=$x, y=$y, xDelta=$xDelta, yDelta=$yDelta")
                FirebaseHelper.logEvent(view.context, "piece_touch_down")
                piece.bringToFront()
            }

            MotionEvent.ACTION_MOVE -> {
                Log.v(
                    TouchListener::class.simpleName,
                    "ACTION_MOVE: x=$x, y=$y, xDelta=$xDelta, yDelta=$yDelta, leftMargin=${lParams.leftMargin}, topMargin=${lParams.topMargin}"
                )
                lParams.leftMargin = (x - xDelta).toInt()
                lParams.topMargin = (y - yDelta).toInt()
                view.setLayoutParams(lParams)
            }

            MotionEvent.ACTION_UP -> {
                val xDiff = StrictMath.abs(piece.xCoord - lParams.leftMargin)
                val yDiff = StrictMath.abs(piece.yCoord - lParams.topMargin)
                Log.v(
                    TouchListener::class.simpleName,
                    "ACTION_UP: x=$x, y=$y, xDelta=$xDelta, yDelta=$yDelta, xDiff=$xDiff, yDiff=$yDiff, tolerance=$tolerance"
                )
                if (xDiff <= tolerance && yDiff <= tolerance) { // piece placed correctly
                    FirebaseHelper.logEvent(view.context, "piece_placed_correctly")
                    AchievementHelper.updateInTheZoneAchievement(puzzleGameManager.activity, true)
                    PlayGamesHelper.unlockAchievement(
                        puzzleGameManager.activity,
                        R.string.achievement_first_piece_down
                    )
                    piece.canMove = false // Prevent further interaction during animation
                    sendViewToBack(piece)
                    val animatorSet = AnimatorSet()
                    animatorSet.playTogether(
                        ObjectAnimator.ofFloat(view, View.X, piece.xCoord.toFloat()),
                        ObjectAnimator.ofFloat(view, View.Y, piece.yCoord.toFloat())
                    )
                    animatorSet.interpolator = AccelerateDecelerateInterpolator()
                    animatorSet.duration = 250 // A quick snap
                    animatorSet.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            // Update layout params to finalize position
                            lParams.leftMargin = piece.xCoord
                            lParams.topMargin = piece.yCoord
                            view.layoutParams = lParams

                            // Reset translation properties modified by the animator
                            view.translationX = 0f
                            view.translationY = 0f

                            puzzleGameManager.checkGameOver()
                            PlayGamesHelper.progressAchievement(puzzleGameManager.activity, R.string.achievement_piece_collector, 1)
                            PlayGamesHelper.progressAchievement(puzzleGameManager.activity, R.string.achievement_piece_enthusiast, 1)
                        }
                    })
                    animatorSet.start()
                } else {
                    AchievementHelper.updateInTheZoneAchievement(puzzleGameManager.activity, false)
                    FirebaseHelper.logEvent(view.context, "piece_placed_incorrectly")
                }
                view.performClick()
            }
        }
        piece.bringToFront()
        return true
    }

    /**
     * Sends a specified view to the back of its parent's view hierarchy.
     * This is used to ensure correctly placed puzzle pieces are visually behind movable pieces.
     * @param child The {@link View} to send to the back.
     */
    private fun sendViewToBack(child: View) {
        val parent = child.parent as ViewGroup
        parent.removeView(child)
        parent.addView(child, 0)
    }
}
