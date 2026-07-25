package com.batodev.jigsawpuzzle

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.batodev.jigsawpuzzle.activity.ImagePickActivity
import org.hamcrest.Matchers.anything
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Covers the real, fully-internal "pick a bundled image" flow (grid -> start
// popup -> start button -> PuzzleActivity). The camera/gallery-picker
// buttons are intentionally not covered - both hand off to real OS-level
// permission dialogs and system pickers entirely outside this app's own
// window, which Espresso isn't suited to drive reliably.
@RunWith(AndroidJUnit4::class)
class ImagePickActivityTest {

    @Before
    fun setUp() {
        resetSettings { lastSetDifficultyCustomWidth = 2; lastSetDifficultyCustomHeight = 4 }
    }

    @Test
    fun gridShowsSelectableImages() {
        val scenario = ActivityScenario.launch(ImagePickActivity::class.java)

        onView(withId(R.id.grid)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun clickingGridItemShowsStartGamePopup() {
        val scenario = ActivityScenario.launch(ImagePickActivity::class.java)

        onData(anything()).inAdapterView(withId(R.id.grid)).atPosition(0).perform(click())

        onView(withId(R.id.startButton)).check(matches(isDisplayed()))
        onView(withId(R.id.difficulty_spinner)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun startButtonLaunchesPuzzleActivity() {
        val scenario = ActivityScenario.launch(ImagePickActivity::class.java)

        onData(anything()).inAdapterView(withId(R.id.grid)).atPosition(0).perform(click())
        onView(withId(R.id.startButton)).perform(click())

        onView(withId(R.id.zoomableLayout)).check(matches(isDisplayed()))
        scenario.close()
    }
}
