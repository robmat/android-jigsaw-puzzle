package dragosholban.com.androidpuzzlegame

import android.media.MediaPlayer
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.RelativeLayout

class TouchListener(private val activity: PuzzleActivity) : OnTouchListener {
    private var xDelta = 0f
    private var yDelta = 0f
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
        val x = motionEvent.rawX
        val y = motionEvent.rawY
        val tolerance = Math.sqrt(
            Math.pow(view.width.toDouble(), 2.0) + Math.pow(
                view.height.toDouble(),
                2.0
            )
        ) / 10
        val piece = view as PuzzlePiece
        if (!piece.canMove) {
            return true
        }
        val lParams = view.getLayoutParams() as RelativeLayout.LayoutParams
        when (motionEvent.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                xDelta = x - lParams.leftMargin
                yDelta = y - lParams.topMargin
                piece.bringToFront()
            }

            MotionEvent.ACTION_MOVE -> {
                lParams.leftMargin = (x - xDelta).toInt()
                lParams.topMargin = (y - yDelta).toInt()
                view.setLayoutParams(lParams)
            }

            MotionEvent.ACTION_UP -> {
                val xDiff = StrictMath.abs(piece.xCoord - lParams.leftMargin)
                val yDiff = StrictMath.abs(piece.yCoord - lParams.topMargin)
                if (xDiff <= tolerance && yDiff <= tolerance) {
                    lParams.leftMargin = piece.xCoord
                    lParams.topMargin = piece.yCoord
                    piece.layoutParams = lParams
                    piece.canMove = false
                    sendViewToBack(piece)
                    activity.checkGameOver()
                    val mp = MediaPlayer.create(activity, okSoundsIds.random())
                    mp.setOnCompletionListener { mp.release() }
                    mp.start()
                }
                view.performClick()
            }
        }
        return true
    }

    private fun sendViewToBack(child: View) {
        val parent = child.parent as ViewGroup
        parent.removeView(child)
        parent.addView(child, 0)
    }
}