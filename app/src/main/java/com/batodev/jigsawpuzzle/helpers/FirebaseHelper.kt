package com.batodev.jigsawpuzzle.helpers

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object FirebaseHelper {

    private fun getAnalytics(context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

    fun logEvent(context: Context, eventName: String, params: Bundle = Bundle()) {
        getAnalytics(context).logEvent(eventName, params)
    }

    fun logScreenView(context: Context, screenName: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        getAnalytics(context).logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logException(context: Context, location: String, message: String?) {
        val bundle = Bundle()
        bundle.putString("location", location)
        bundle.putString("message", message ?: "No message")
        logEvent(context, "exception_caught", bundle)
    }

    fun logButtonClick(context: Context, buttonName: String) {
        val bundle = Bundle()
        bundle.putString("button_name", buttonName)
        logEvent(context, "button_click", bundle)
    }
}