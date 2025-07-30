package com.batodev.jigsawpuzzle.helpers

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import com.google.android.play.core.review.ReviewException
import java.util.Random

class AppRatingHelper(private val activity: Activity) {

    private val manager: ReviewManager by lazy {
        // return@lazy FakeReviewManager(activity)
        return@lazy ReviewManagerFactory.create(activity)
    }

    fun requestReview() {
        if (Random().nextInt(5) == 0) {
            try {
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // We got the ReviewInfo object
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(activity, reviewInfo)
                        flow.addOnCompleteListener { res ->
                            // The flow has finished. The API does not indicate whether the user
                            // reviewed or not, or even whether the review dialog was shown. Thus, no
                            // matter the result, we continue our app flow.
                            Log.i(AppRatingHelper::class.java.simpleName, "review ok: $res")
                        }
                    } else {
                        // There was some problem, log or handle the error code.
                        @ReviewErrorCode val reviewErrorCode = (task.exception as ReviewException).errorCode
                        Log.w(AppRatingHelper::class.java.simpleName, "review ko: ${task.exception} $reviewErrorCode")
                    }
                }

            } catch (e: Exception) {
                // Handle error (e.g., log or fallback to browser)
                Log.w(AppRatingHelper::class.java.simpleName, "Error requesting review: ${e.message}")
                fallbackToPlayStore()
            }
        }
    }

    private fun fallbackToPlayStore() {
        // Fallback to opening Play Store if in-app review fails
        val packageName = activity.packageName
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
            )
        } catch (e: ActivityNotFoundException) {
            Log.w(AppRatingHelper::class.java.simpleName, "Error requesting review: ${e.message}")
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri())
            )
        }
    }
}
