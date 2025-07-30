package com.batodev.jigsawpuzzle.view

import android.annotation.SuppressLint
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
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var scaleFactor = 1.0f
    private val gestureDetector: ScaleGestureDetector

    companion object {
        private const val MIN_ZOOM = 1.0f
        private const val MAX_ZOOM = 2.0f
    }

    init {
        gestureDetector = ScaleGestureDetector(context, ScaleListener())
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
            Log.d(ZoomableLayout::class.simpleName, "onScale: scaleFactor=$scaleFactor")
            return true
        }
    }

    // For two-finger panning
    private var lastPanX = 0f
    private var lastPanY = 0f
    private var isPanning = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Intercept only for two or more fingers (for zoom/pan)
        if (ev.pointerCount > 1) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        val isScaling = gestureDetector.isInProgress
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPanning = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2 && !isScaling) {
                    // Start panning
                    lastPanX = (event.getX(0) + event.getX(1)) / 2f
                    lastPanY = (event.getY(0) + event.getY(1)) / 2f
                    isPanning = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isPanning && event.pointerCount == 2 && !isScaling) {
                    val panX = (event.getX(0) + event.getX(1)) / 2f
                    val panY = (event.getY(0) + event.getY(1)) / 2f
                    val PAN_SPEED = 1.6f
                    val dx = (panX - lastPanX) * PAN_SPEED
                    val dy = (panY - lastPanY) * PAN_SPEED
                    translationX += dx
                    translationY += dy
                    checkAndApplyTranslation()
                    lastPanX = panX
                    lastPanY = panY
                    Log.d(ZoomableLayout::class.simpleName, "onTouchEvent: Panning with dx=$dx, dy=$dy")
                }
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.pointerCount <= 2) {
                    isPanning = false
                }
            }
        }
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
