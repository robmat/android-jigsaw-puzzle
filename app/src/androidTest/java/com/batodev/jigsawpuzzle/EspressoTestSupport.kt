package com.batodev.jigsawpuzzle

import android.graphics.drawable.Animatable
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoActivityResumedException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.batodev.jigsawpuzzle.helpers.Settings
import com.batodev.jigsawpuzzle.helpers.SettingsHelper
import com.batodev.jigsawpuzzle.view.PuzzlePiece
import com.otaliastudios.zoom.ZoomLayout
import org.hamcrest.Matcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals

// Shared across MainMenuActivityTest/ImagePickActivityTest/PuzzleActivityTest/GalleryActivityTest.
// Ported from AndroidPuzzleCuties (this app's near-identical sibling), whose Espresso gotchas
// (a looping confetti GIF starving idle-detection, a synchronously-shown high-score AlertDialog
// unconditionally requiring window focus regardless of a custom inRoot() matcher) all apply here
// too - see AndroidPuzzleCuties's RELEASE_TRACKER.md entry for the original diagnosis. None of
// this app's Activities override back-press, so the standard pattern applies everywhere.

fun assertEventuallyDestroyed(scenario: ActivityScenario<*>, timeoutMs: Long = 8_000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (scenario.state != Lifecycle.State.DESTROYED && System.currentTimeMillis() < deadline) {
        Thread.sleep(50)
    }
    assertEquals(Lifecycle.State.DESTROYED, scenario.state)
}

fun assertBackPressFinishesScenario(scenario: ActivityScenario<*>) {
    try {
        pressBack()
    } catch (expected: NoActivityResumedException) {
    }
    assertEventuallyDestroyed(scenario)
}

/**
 * SettingsHelper is plain SharedPreferences-backed, not tied to any particular
 * Activity instance, so settings can be seeded directly from the instrumentation's
 * own targetContext before ever launching an Activity. Also clears the
 * saved_game directory PuzzleActivity/MainMenuActivity's "continue game"
 * feature relies on (File(filesDir, "saved_game/gamestate.json")) - a new
 * feature this app has that AndroidPuzzleCuties doesn't - so tests always
 * start from a deterministic no-save-exists state unless a test explicitly
 * wants otherwise.
 */
fun resetSettings(configure: Settings.() -> Unit = {}) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    context.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
    SettingsHelper.save(context, Settings().apply(configure))
    java.io.File(context.filesDir, "saved_game").deleteRecursively()
}

/**
 * MainMenuActivity's buttons start INVISIBLE and only become VISIBLE via a
 * Handler.postDelayed(..., 500) animation kickoff in onResume() - a small
 * safety margin wait before the first interaction avoids racing that delay.
 */
fun waitFor(millis: Long): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isRoot()
    override fun getDescription(): String = "wait for ${millis}ms while pumping the main looper"
    override fun perform(uiController: UiController, view: View) {
        uiController.loopMainThreadForAtLeast(millis)
    }
}

/**
 * onGameOver() loads confetti2 as an animated GIF into konfettiView via
 * Glide. A playing GIF keeps scheduling new Choreographer frame callbacks
 * indefinitely, which can starve Espresso's "main looper has idled" check
 * that every subsequent ViewAction/ViewAssertion depends on -
 * AppNotIdleException after a full 60s timeout if a click happens to land
 * while it's still animating. Deliberately *not* a ViewAction here: if the
 * looping GIF is what's blocking idle detection, a normal perform() call
 * would itself hang waiting for idle before ever reaching the stop() call
 * inside it. ActivityScenario.onActivity{} runs via Instrumentation's
 * runOnMainSync() instead, which only waits for this specific Runnable to
 * finish - not for the whole app to go idle first - so it can reach in and
 * stop the drawable (Glide's GifDrawable implements Animatable) regardless.
 */
fun stopGifAnimation(scenario: ActivityScenario<*>, viewId: Int) {
    scenario.onActivity { activity ->
        val target = activity.findViewById<ImageView>(viewId)
        (target.drawable as? Animatable)?.stop()
    }
}

/**
 * PuzzleActivity cuts pieces asynchronously on a background thread pool
 * (PuzzleCutter.default().cut(), a MaskBitmapPuzzleCutter here) and only
 * flips progressBar to GONE once every piece's bitmap/xCoord/yCoord have
 * been posted back to the main thread - polls until that happens rather
 * than gambling on a fixed wait, since cut time scales with image
 * size/piece count/device load. This app also fakes some progress ticks via
 * a separate Handler.postDelayed loop (fakeSomeProgress()) purely for UX,
 * which doesn't affect when the real GONE transition happens.
 */
fun waitUntilPuzzleReady(timeoutMs: Long = 30_000): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isRoot()
    override fun getDescription(): String =
        "wait until PuzzleActivity's progressBar becomes GONE (cutting finished and pieces scattered)"
    override fun perform(uiController: UiController, view: View) {
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (progressBar.visibility != View.GONE && System.currentTimeMillis() < deadline) {
            uiController.loopMainThreadForAtLeast(200)
        }
        check(progressBar.visibility == View.GONE) { "puzzle cutting never finished within ${timeoutMs}ms" }
    }
}

/**
 * Solves the real puzzle for real: PuzzlePiece exposes its own correct
 * xCoord/yCoord/canMove as plain public fields (set by the cutter once
 * cutting finishes), and TouchListener.onTouch() - the app's real drag
 * handler - only cares about a piece's LayoutParams margins matching
 * xCoord/yCoord within a tolerance at ACTION_UP, not about the actual path
 * a real finger would have dragged through. So rather than fake a finger
 * drag with realistic intermediate movement, this dispatches ACTION_DOWN at
 * the piece's current position (making TouchListener's internal
 * xDelta/yDelta exactly 0) followed by ACTION_MOVE/ACTION_UP at exactly its
 * target xCoord/yCoord (scaled by the current zoom, since TouchListener
 * divides rawX/rawY by zoomableLayout.zoom) - which lands the piece exactly
 * on target and genuinely triggers the app's own placement-check/snap-
 * animation/checkGameOver() code path for every piece, the same way a
 * perfectly accurate real drag would.
 */
fun solvePuzzle(): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> =
        org.hamcrest.core.IsInstanceOf.instanceOf(ZoomLayout::class.java)

    override fun getDescription(): String =
        "drag every PuzzlePiece under this ZoomLayout to its correct xCoord/yCoord via synthetic touch events"

    override fun perform(uiController: UiController, view: View) {
        val zoomLayout = view as ZoomLayout
        val layout = zoomLayout.getChildAt(0) as RelativeLayout
        val zoom = zoomLayout.zoom
        val pieces = (0 until layout.childCount).mapNotNull { layout.getChildAt(it) as? PuzzlePiece }
        check(pieces.isNotEmpty()) { "no PuzzlePiece children found under the puzzle layout" }

        for (piece in pieces) {
            val lParams = piece.layoutParams as RelativeLayout.LayoutParams
            val downTime = SystemClock.uptimeMillis()
            val downX = lParams.leftMargin.toFloat() * zoom
            val downY = lParams.topMargin.toFloat() * zoom
            val targetX = piece.xCoord.toFloat() * zoom
            val targetY = piece.yCoord.toFloat() * zoom

            val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, downX, downY, 0)
            piece.dispatchTouchEvent(down)
            down.recycle()

            val move = MotionEvent.obtain(
                downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, targetX, targetY, 0
            )
            piece.dispatchTouchEvent(move)
            move.recycle()

            val up = MotionEvent.obtain(
                downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, targetX, targetY, 0
            )
            piece.dispatchTouchEvent(up)
            up.recycle()

            // Let the real 250ms snap-into-place animation (and, for the
            // last piece, checkGameOver()'s follow-on work) actually finish
            // before touching the next piece.
            uiController.loopMainThreadForAtLeast(350)
        }
        uiController.loopMainThreadUntilIdle()
    }
}
