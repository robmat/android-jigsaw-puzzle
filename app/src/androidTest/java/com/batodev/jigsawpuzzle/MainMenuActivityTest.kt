package com.batodev.jigsawpuzzle

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.batodev.jigsawpuzzle.activity.MainMenuActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Covers the buttons on main_menu_activity.xml: play, gallery (both the
// empty-state Snackbar and the real-navigation case), more-apps, and
// play-part-2. All NeonButtons start View.INVISIBLE and only become visible
// via a Handler.postDelayed(..., 500) animation kickoff in onResume() -
// every test waits that out first via waitFor(). The achievements button
// (which drives a real Google Play Games sign-in flow) is only checked for
// presence, never clicked - same "don't drive real external-service flows"
// judgment already applied to the camera/gallery image pickers in
// ImagePickActivityTest. "Continue game" is left GONE by resetSettings()
// deleting any saved_game directory, so it's not separately tested here.
@RunWith(AndroidJUnit4::class)
class MainMenuActivityTest {

    @Before
    fun setUp() {
        resetSettings()
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    private fun launchSettled(): ActivityScenario<MainMenuActivity> {
        val scenario = ActivityScenario.launch(MainMenuActivity::class.java)
        onView(isRoot()).perform(waitFor(600))
        return scenario
    }

    @Test
    fun playButtonOpensImagePickActivity() {
        val scenario = launchSettled()

        onView(withId(R.id.main_menu_activity_play_the_game)).perform(click())

        onView(withId(R.id.grid)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun continueGameButtonHiddenWithNoSavedGame() {
        val scenario = launchSettled()

        onView(withId(R.id.main_menu_activity_continue_game)).check(matches(withEffectiveVisibility(GONE)))
        scenario.close()
    }

    @Test
    fun galleryButtonShowsSnackbarWithNoUncoveredPics() {
        val scenario = launchSettled()

        onView(withId(R.id.main_menu_activity_unlocked_gallery)).perform(click())

        onView(withText(R.string.main_menu_activity_play_to_uncover)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun galleryButtonOpensGalleryActivityWithUncoveredPics() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val realPic = context.assets.list("img")!!.first()
        resetSettings { uncoveredPics = mutableListOf(realPic) }
        val scenario = launchSettled()

        onView(withId(R.id.main_menu_activity_unlocked_gallery)).perform(click())

        onView(withId(R.id.gallery_activity_background)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun moreAppsButtonOpensDeveloperPlayStorePage() {
        val scenario = launchSettled()

        onView(withId(R.id.main_menu_activity_more_apps)).perform(click())

        intended(hasAction(Intent.ACTION_VIEW))
        intended(hasData(Uri.parse("https://play.google.com/store/apps/dev?id=8228670503574649511")))
        scenario.close()
    }

    @Test
    fun playPart2ButtonOpensPlayStorePageForPart2() {
        val scenario = launchSettled()

        onView(withId(R.id.main_menu_activity_play_part_2)).perform(click())

        intended(hasAction(Intent.ACTION_VIEW))
        intended(hasData(Uri.parse("https://play.google.com/store/apps/details?id=com.batodev.jigsawpuzzle3")))
        scenario.close()
    }

    @Test
    fun achievementsButtonIsPresent() {
        val scenario = launchSettled()

        onView(withId(R.id.main_menu_activity_achievements)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun systemBackPressFinishesActivity() {
        val scenario = launchSettled()

        assertBackPressFinishesScenario(scenario)
    }
}
