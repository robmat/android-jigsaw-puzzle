package com.batodev.jigsawpuzzle

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.batodev.jigsawpuzzle.activity.GalleryActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// GalleryActivity reads settings.uncoveredPics directly (no empty-list
// guard in onCreate() - setImage(images[index]) would throw
// IndexOutOfBoundsException on an empty list, though not reachable through
// real UI since MainMenuActivity's own gallery button already gates on a
// non-empty list first), so uncoveredPics is always seeded with real
// assets/img filenames before every launch here.
@RunWith(AndroidJUnit4::class)
class GalleryActivityTest {
    private lateinit var images: List<String>

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        images = context.assets.list("img")!!.take(3)
        resetSettings { uncoveredPics = images.toMutableList() }
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    private fun launch(): ActivityScenario<GalleryActivity> = ActivityScenario.launch(GalleryActivity::class.java)

    @Test
    fun launchesShowingFirstImageWithLeftHiddenAndRightVisible() {
        val scenario = launch()

        onView(withId(R.id.gallery_activity_background)).check(matches(isDisplayed()))
        onView(withId(R.id.gallery_left)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.gallery_right)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun rightClickTwiceReachesLastImageHidingRightShowingLeft() {
        val scenario = launch()

        onView(withId(R.id.gallery_right)).perform(click())
        onView(isRoot()).perform(waitFor(500))
        onView(withId(R.id.gallery_right)).perform(click())
        onView(isRoot()).perform(waitFor(500))

        onView(withId(R.id.gallery_right)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.gallery_left)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun shareButtonSharesCurrentImage() {
        val scenario = launch()

        onView(withId(R.id.gallery_share_btn)).perform(click())

        intended(hasAction(Intent.ACTION_SEND))
        intended(hasType("image/*"))
        scenario.close()
    }

    @Test
    fun wallpaperButtonSetsWallpaperWithoutCrashing() {
        val scenario = launch()

        onView(withId(R.id.gallery_wallpaper_btn)).perform(click())

        onView(withId(R.id.gallery_activity_background)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun backButtonFinishesActivity() {
        val scenario = launch()

        onView(withId(R.id.gallery_back_btn)).perform(click())

        assertEventuallyDestroyed(scenario)
    }

    @Test
    fun systemBackPressFinishesActivity() {
        val scenario = launch()

        assertBackPressFinishesScenario(scenario)
    }
}
