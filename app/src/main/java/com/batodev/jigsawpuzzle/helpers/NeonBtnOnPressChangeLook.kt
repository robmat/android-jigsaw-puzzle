package com.batodev.jigsawpuzzle.helpers

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.batodev.jigsawpuzzle.R
import com.smb.glowbutton.NeonButton

object NeonBtnOnPressChangeLook {
    fun neonBtnOnPressChangeLook(
        view: View,
        event: MotionEvent,
        appCompatActivity: AppCompatActivity
    ) {
        val nb: NeonButton = view as NeonButton
        when (event.action) {
            /**
             *             app:nb_gradientEnd="@color/colorAccent"
             *             app:nb_gradientStart="@color/colorPrimary"
             */
            MotionEvent.ACTION_DOWN -> {
                nb.gradientStart = appCompatActivity.getColor(R.color.colorAccentDark)
                nb.gradientEnd = appCompatActivity.getColor(R.color.colorPrimary)
            }

            MotionEvent.ACTION_UP -> {
                nb.gradientEnd = appCompatActivity.getColor(R.color.colorAccent)
                nb.gradientStart = appCompatActivity.getColor(R.color.colorPrimary)
                nb.performClick()
            }

            MotionEvent.ACTION_CANCEL -> {
                nb.gradientEnd = appCompatActivity.getColor(R.color.colorAccent)
                nb.gradientStart = appCompatActivity.getColor(R.color.colorPrimary)
            }
        }
        nb.disable()
        nb.enable()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupNeonButtonTouchListeners(activity: AppCompatActivity, vararg buttons: NeonButton) {
        for (button in buttons) {
            button.setOnTouchListener { view, event ->
                neonBtnOnPressChangeLook(
                    view,
                    event,
                    activity
                )
                true
            }
        }
    }

}
