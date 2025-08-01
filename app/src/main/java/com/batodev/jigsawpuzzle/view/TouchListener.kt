package com.batodev.jigsawpuzzle.view

import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.batodev.jigsawpuzzle.logic.PuzzleGameManager
import com.otaliastudios.zoom.ZoomLayout
import kotlin.math.pow
import kotlin.math.sqrt

class TouchListener(
    private val puzzleGameManager: PuzzleGameManager,
    private val zoomableLayout: ZoomLayout
) : OnTouchListener {
    private var xDelta = 0f
    private var yDelta = 0f

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
                if (xDiff <= tolerance && yDiff <= tolerance) {
                    lParams.leftMargin = piece.xCoord
                    lParams.topMargin = piece.yCoord
                    piece.layoutParams = lParams
                    piece.canMove = false
                    sendViewToBack(piece)
                    puzzleGameManager.checkGameOver()
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
