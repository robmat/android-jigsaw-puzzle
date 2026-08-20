package com.batodev.jigsawpuzzle.helpers

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import java.util.Random

/**
 * A helper class for requesting app ratings.
 */
class AppRatingHelper(
    private val activity: Activity,
) {
    companion object {
        private const val REVIEW_REQUEST_ODDS = 5
    }

    private val manager: ReviewManager by lazy {
        // return@lazy FakeReviewManager(activity)
        return@lazy ReviewManagerFactory.create(activity)
    }

    /**
     * Requests an in-app review from the user. The review dialog is shown randomly.
     * If the in-app review fails, it falls back to opening the app's page in the Play Store.
     */
    fun requestReview() {
        if (Random().nextInt(REVIEW_REQUEST_ODDS) == 0) {
            FirebaseHelper.logEvent(activity, "request_review")
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
                            FirebaseHelper.logEvent(activity, "review_flow_complete")
                            Log.i(AppRatingHelper::class.java.simpleName, "review ok: $res")
                        }
                    } else {
                        // There was some problem, log or handle the error code.
                        @ReviewErrorCode val reviewErrorCode = (task.exception as ReviewException).errorCode
                        FirebaseHelper.logException(activity, "request_review_failed", task.exception?.message)
                        Log.w(AppRatingHelper::class.java.simpleName, "review ko: ${task.exception} $reviewErrorCode")
                    }
                }
            } catch (e: IllegalStateException) {
                // Play Core throws this when the Play Store app isn't installed/available
                // on the device (e.g. emulators without Play Services).
                FirebaseHelper.logException(activity, "request_review_exception", e.message)
                Log.w(AppRatingHelper::class.java.simpleName, "Error requesting review: ${e.message}")
                fallbackToPlayStore()
            }
        }
    }

    /**
     * Falls back to opening the app's page in the Google Play Store.
     * This method is called if the in-app review flow encounters an error.
     */
    private fun fallbackToPlayStore() {
        // Fallback to opening Play Store if in-app review fails
        FirebaseHelper.logEvent(activity, "fallback_to_play_store")
        val packageName = activity.packageName
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()),
            )
        } catch (e: ActivityNotFoundException) {
            FirebaseHelper.logException(activity, "fallback_to_play_store_failed", e.message)
            Log.w(AppRatingHelper::class.java.simpleName, "Error requesting review: ${e.message}")
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()),
            )
        }
    }
}
