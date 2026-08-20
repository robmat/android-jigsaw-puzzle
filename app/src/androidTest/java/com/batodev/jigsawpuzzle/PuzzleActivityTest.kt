package com.batodev.jigsawpuzzle

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.batodev.jigsawpuzzle.activity.PuzzleActivity
import com.batodev.jigsawpuzzle.helpers.SettingsHelper
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.sameInstance
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// PuzzleActivity is launched directly via the same "assetName"/"newGame"
// intent extras ImagePickActivity.startTheGame() passes in real play, using
// a real assets/img filename so the real cutting/decoding pipeline runs for
// real - covered separately from ImagePickActivityTest's own coverage of
// reaching this Activity through the picker UI. "newGame"=true matters here
// specifically: without it, onCreate() would try to resume from any
// leftover saved_game/gamestate.json instead of cutting a fresh puzzle from
// assetName (resetSettings() already deletes that directory too, belt and
// suspenders).
//
// The difficulty is seeded to 2x4 (8 pieces, the smallest the app's own
// spinner offers) to keep solvePuzzle()'s per-piece settle wait bounded.
@RunWith(AndroidJUnit4::class)
class PuzzleActivityTest {
    private lateinit var assetName: String

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assetName = context.assets.list("img")!!.first()
        resetSettings {
            lastSetDifficultyCustomWidth = 2
            lastSetDifficultyCustomHeight = 4
        }
    }

    private fun launchWithAsset(): ActivityScenario<PuzzleActivity> {
        val intent =
            Intent(
                InstrumentationRegistry.getInstrumentation().targetContext,
                PuzzleActivity::class.java,
            ).apply {
                putExtra("assetName", assetName)
                putExtra("newGame", true)
            }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun launchesCutsAndScattersPieces() {
        val scenario = launchWithAsset()

        onView(isRoot()).perform(waitUntilPuzzleReady())

        onView(withId(R.id.zoomableLayout)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun solvingPuzzleTriggersGameOverShowsHighScorePopupAndPlayAgainFinishes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scenario = launchWithAsset()
        onView(isRoot()).perform(waitUntilPuzzleReady())

        onView(withId(R.id.zoomableLayout)).perform(solvePuzzle())
        // Stop the confetti GIF Glide starts playing in onGameOver() before
        // any further interaction - see stopGifAnimation()'s kdoc.
        stopGifAnimation(scenario, R.id.konfettiView)

        // onGameOver() shows the high-score AlertDialog synchronously as its
        // very last step, which steals window focus from the Activity -
        // Espresso's root picker unconditionally waits for whichever root a
        // check/perform targets to actually hold window focus (a custom
        // inRoot() matcher only narrows which root is *eligible*, it doesn't
        // skip the focus-wait), so the dialog must be dismissed first while
        // it's still the focused root, before the Activity's own views
        // become checkable again.
        var activityDecorView: View? = null
        scenario.onActivity { activity -> activityDecorView = activity.window.decorView }
        val dialogRoot = withDecorView(not(sameInstance(activityDecorView)))
        onView(withId(R.id.highScoreOkButton)).inRoot(dialogRoot).perform(click())

        onView(withId(R.id.konfettiView)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.puzzle_activity_play_again)).check(matches(withEffectiveVisibility(VISIBLE)))
        assertTrue(SettingsHelper.load(context).uncoveredPics.contains(assetName))

        onView(withId(R.id.puzzle_activity_play_again)).perform(click())

        // A longer margin than the default 8s: this test class does
        // genuinely heavy CPU work (image cutting, flood fill, GIF decode),
        // so its tail end is more exposed than most to transient slowness
        // under cumulative full-suite load.
        assertEventuallyDestroyed(scenario, timeoutMs = 15_000)
    }

    @Test
    fun systemBackPressFinishesActivity() {
        val scenario = launchWithAsset()

        assertBackPressFinishesScenario(scenario)
    }
}
