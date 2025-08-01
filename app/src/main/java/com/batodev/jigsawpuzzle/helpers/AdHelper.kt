package com.batodev.jigsawpuzzle.helpers

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

const val AD_ID = "ca-app-pub-9667420067790140/7403183964"

/**
 * A helper object for displaying ads.
 */
object AdHelper {
    private var ad: InterstitialAd? = null

    /**
     * Shows an interstitial ad if the ad counter reaches the display threshold.
     * The ad counter is reset after showing the ad.
     * @param activity The {@link Activity} context used to show the ad.
     * @see SettingsHelper
     * @see #showAd(Activity)
     */
    fun showAdIfNeeded(activity: Activity) {
        val settings = SettingsHelper.load(activity)
        if (settings.addCounter >= settings.displayAddEveryXPicView) {
            settings.addCounter = 0
            SettingsHelper.save(activity, settings)
            showAd(activity)
        }
    }

    /**
     * Displays the loaded interstitial ad and then reloads a new ad.
     * @param activity The {@link Activity} context used to show the ad.
     * @see #loadAd(Activity)
     */
    fun showAd(activity: Activity) {
        ad?.show(activity)
        loadAd(activity)
    }

    /**
     * Loads an interstitial ad. The loaded ad is stored in a private variable.
     * @param activity The {@link Activity} context used to load the ad.
     */
    fun loadAd(activity: Activity) {
        val adRequest: AdRequest = AdRequest.Builder().build()

        InterstitialAd.load(activity, AD_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    // The mInterstitialAd reference will be null until
                    // an ad is loaded.
                    Log.i(AdHelper::class.simpleName, "onAdLoaded: $interstitialAd")
                    ad = interstitialAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error
                    Log.w(AdHelper::class.simpleName, "onAdFailedToLoad: $loadAdError")
                }
            })
    }
}
