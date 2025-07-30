package com.batodev.jigsawpuzzle.view

import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.helpers.SoundsPlayer
import com.batodev.jigsawpuzzle.activity.PuzzleActivity
import kotlin.math.pow
import kotlin.math.sqrt

class TouchListener(
    private val activity: PuzzleActivity, private val zoomableLayout: ZoomableLayout
) : OnTouchListener {
    private var xDelta = 0f
    private var yDelta = 0f

    // We need to track the start raw coordinates to calculate the total drag distance
    private var rawXStart = 0f
    private var rawYStart = 0f

    private val okSoundsIds = listOf(
        R.raw.ok_1,
        R.raw.ok_2,
        R.raw.ok_3,
        R.raw.ok_4,
        R.raw.ok_5,
        R.raw.ok_6,
        R.raw.ok_7,
        R.raw.ok_8,
        R.raw.ok_9,
        R.raw.ok_10,
        R.raw.ok_11,
        R.raw.ok_12,
        R.raw.ok_13,
        R.raw.ok_14,
        R.raw.ok_15,
        R.raw.ok_16,
        R.raw.ok_17,
        R.raw.ok_18
    )

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        // We use rawX and rawY for screen coordinates, which are independent of zoom.
        val rawX = motionEvent.rawX
        val rawY = motionEvent.rawY
        val piece = view as PuzzlePiece
        if (!piece.canMove) {
            return true
        }

        val lParams = view.layoutParams as RelativeLayout.LayoutParams
        when (motionEvent.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                rawXStart = rawX
                rawYStart = rawY

                // xDelta and yDelta are the offsets from the view's top-left corner to the touch point.
                xDelta = lParams.leftMargin.toFloat()
                yDelta = lParams.topMargin.toFloat()
            }

            MotionEvent.ACTION_MOVE -> {
                // Calculate the displacement on the screen
                val dx = rawX - rawXStart
                val dy = rawY - rawYStart

                // Adjust the displacement by the current scale factor
                // This makes the piece move correctly relative to the zoomed view.
                val scale = zoomableLayout.getScaleFactor()
                lParams.leftMargin = (xDelta + dx / scale).toInt()
                lParams.topMargin = (yDelta + dy / scale).toInt()
                view.layoutParams = lParams
            }

            MotionEvent.ACTION_UP -> {
                val tolerance =
                    sqrt(view.width.toDouble().pow(2.0) + view.height.toDouble().pow(2.0)) / 10
                val xDiff = StrictMath.abs(piece.xCoord - lParams.leftMargin)
                val yDiff = StrictMath.abs(piece.yCoord - lParams.topMargin)
                if (xDiff <= tolerance && yDiff <= tolerance) {
                    lParams.leftMargin = piece.xCoord
                    lParams.topMargin = piece.yCoord
                    piece.layoutParams = lParams
                    piece.canMove = false
                    sendViewToBack(piece)
                    activity.checkGameOver()
                    SoundsPlayer.play(okSoundsIds.random(), activity)
                }
                view.performClick()
            }
        }
        piece.bringToFront()
        return true
    }

    private fun sendViewToBack(child: View) {
        val parent = child.parent as ViewGroup
        parent.removeView(child)
        parent.addView(child, 0)
    }
}
