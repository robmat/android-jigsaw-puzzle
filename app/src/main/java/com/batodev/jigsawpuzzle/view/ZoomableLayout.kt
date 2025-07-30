package com.batodev.jigsawpuzzle.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.RelativeLayout
import kotlin.math.max
import kotlin.math.min

class ZoomableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var scaleFactor = 1.0f
    private val gestureDetector: ScaleGestureDetector

    companion object {
        private const val MIN_ZOOM = 1.0f
        private const val MAX_ZOOM = 4.0f
    }

    init {
        gestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        Log.d(ZoomableLayout::class.simpleName, "performClick called")
        return super.performClick()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(MIN_ZOOM, min(scaleFactor, MAX_ZOOM))

            // Apply scale and translation
            scaleX = scaleFactor
            scaleY = scaleFactor

            checkAndApplyTranslation()
            return true
        }
    }

    // These variables track the touch gesture for panning
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Intercept touch events for panning when scaled
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            // Reset tracking
            lastTouchX = ev.x
            lastTouchY = ev.y
            activePointerId = ev.getPointerId(0)
        } else if (scaleFactor > MIN_ZOOM) {
            // If zoomed in, intercept move events to handle panning
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        super.dispatchTouchEvent(ev) // Ensure children receive touch events

        val action = ev.actionMasked
        val pointerIndex = ev.findPointerIndex(activePointerId)
        if (pointerIndex < 0) return true

        val currentX = ev.getX(pointerIndex)
        val currentY = ev.getY(pointerIndex)

        when (action) {
            MotionEvent.ACTION_MOVE -> {
                if (scaleFactor > MIN_ZOOM) { // Only pan if zoomed
                    val dx = currentX - lastTouchX
                    val dy = currentY - lastTouchY
                    translationX += dx
                    translationY += dy
                    checkAndApplyTranslation()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val upPointerIndex = ev.actionIndex
                if (ev.getPointerId(upPointerIndex) == activePointerId) {
                    val newPointerIndex = if (upPointerIndex == 0) 1 else 0
                    lastTouchX = ev.getX(newPointerIndex)
                    lastTouchY = ev.getY(newPointerIndex)
                    activePointerId = ev.getPointerId(newPointerIndex)
                }
            }
        }
        lastTouchX = currentX
        lastTouchY = currentY
        gestureDetector.onTouchEvent(ev) // Also feed events to scale detector
        return true
    }

    private fun checkAndApplyTranslation() {
        post {
            val scaledWidth = width * scaleFactor
            val scaledHeight = height * scaleFactor

            val xBound = (scaledWidth - width) / 2
            val yBound = (scaledHeight - height) / 2

            translationX = max(-xBound, min(translationX, xBound))
            translationY = max(-yBound, min(translationY, yBound))
        }
    }

    fun getScaleFactor(): Float {
        return scaleFactor
    }
}
