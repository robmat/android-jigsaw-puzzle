package com.batodev.jigsawpuzzle.logic

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.activity.PuzzleActivity
import com.batodev.jigsawpuzzle.cut.PuzzleCurvesGenerator
import com.batodev.jigsawpuzzle.cut.PuzzleCutRequest
import com.batodev.jigsawpuzzle.cut.PuzzleCutter
import com.batodev.jigsawpuzzle.helpers.Settings
import com.batodev.jigsawpuzzle.helpers.SoundsPlayer
import com.batodev.jigsawpuzzle.view.PuzzlePiece
import com.batodev.jigsawpuzzle.view.TouchListener
import com.otaliastudios.zoom.ZoomLayout
import java.util.Random

/**
 * A class for managing the puzzle game logic.
 */
class PuzzleGameManager(
    val activity: AppCompatActivity,
    private val layout: RelativeLayout,
    private val imageView: ImageView,
    private val zoomableLayout: ZoomLayout,
    private val settings: Settings,
    private val puzzleProgressListener: PuzzleProgressListener
) {
    companion object {
        private const val PIECE_OVERLAP_DIVISOR = 3
        private const val PIECE_X_OFFSET = 4
        private const val PIECE_Y_OFFSET = 7
        private const val SCATTER_TOP_MARGIN = 10
        private const val SCATTER_ANIMATION_DURATION_MS = 500L
        private const val SCATTER_MAX_START_DELAY_MS = 1501
    }

    var pieces: MutableList<PuzzlePiece> = mutableListOf()
    var svgString: String? = null
    private val winSoundIds = listOf(
        R.raw.success_1,
        R.raw.success_2,
        R.raw.success_3,
        R.raw.success_4,
        R.raw.success_5,
        R.raw.success_6
    )
    private val okSoundsIds = listOf(
        R.raw.ok_1, R.raw.ok_2, R.raw.ok_3, R.raw.ok_4, R.raw.ok_5, R.raw.ok_6, R.raw.ok_7, R.raw.ok_8,
        R.raw.ok_9, R.raw.ok_10, R.raw.ok_11, R.raw.ok_12, R.raw.ok_13, R.raw.ok_14, R.raw.ok_15,
        R.raw.ok_16, R.raw.ok_17, R.raw.ok_18
    )

    /**
     * Creates the puzzle pieces from the given bitmap and initializes their positions.
     * It also draws the puzzle grid and/or background image on the ImageView based on settings.
     * @param bitmap The source {@link Bitmap} to create puzzle pieces from.
     * @param puzzlesWidth The desired number of puzzle pieces horizontally.
     * @param puzzlesHeight The desired number of puzzle pieces vertically.
     * @see PuzzleCurvesGenerator
     * @see PuzzleCutter
     * @see PuzzlePiece
     */
    fun createPuzzle(bitmap: Bitmap, puzzlesWidth: Int, puzzlesHeight: Int) {
        val puzzleCurvesGenerator = PuzzleCurvesGenerator()
        puzzleCurvesGenerator.width = bitmap.width.toDouble()
        puzzleCurvesGenerator.height = bitmap.height.toDouble()
        puzzleCurvesGenerator.xn = puzzlesWidth.toDouble()
        puzzleCurvesGenerator.yn = puzzlesHeight.toDouble()
        this.svgString = puzzleCurvesGenerator.generateSvg()

        PuzzleBackgroundRenderer.draw(imageView, bitmap, this.svgString, settings)

        val pieceWidth = bitmap.width / puzzlesWidth
        val pieceHeight = bitmap.height / puzzlesHeight

        var yCoord = 0
        for (row in 0 until puzzlesHeight) {
            var xCoord = 0
            for (col in 0 until puzzlesWidth) {
                var offsetX = 0
                var offsetY = 0
                if (col > 0) {
                    offsetX = pieceWidth / PIECE_OVERLAP_DIVISOR
                }
                if (row > 0) {
                    offsetY = pieceHeight / PIECE_OVERLAP_DIVISOR
                }

                val piece = PuzzlePiece(activity)
                piece.xCoord = xCoord - offsetX + imageView.left + PIECE_X_OFFSET
                piece.yCoord = yCoord - offsetY + imageView.top + PIECE_Y_OFFSET
                piece.pieceWidth = pieceWidth + offsetX
                piece.pieceHeight = pieceHeight + offsetY
                pieces.add(piece)
                xCoord += pieceWidth
            }
            yCoord += pieceHeight
        }
        PuzzleCutter.default().cut(
            PuzzleCutRequest(
                sourceImage = bitmap,
                rows = puzzlesHeight,
                cols = puzzlesWidth,
                svgString = this.svgString,
                imageView = imageView,
                puzzleProgressListener = puzzleProgressListener,
                pieces = pieces
            )
        )
    }

    /**
     * Randomly scatters the puzzle pieces on the game layout.
     * Each piece is assigned a random position within the layout boundaries.
     * @see TouchListener
     * @see PuzzlePiece
     */
    fun scatterPieces() {
        val touchListener = TouchListener(this, zoomableLayout)
        pieces.shuffle()
        for (piece in pieces) {
            piece.setOnTouchListener(touchListener)
            layout.addView(piece)

            // Initial state for animation
            piece.scaleX = 0f
            piece.scaleY = 0f

            val lParams = piece.layoutParams as RelativeLayout.LayoutParams
            lParams.leftMargin = Random().nextInt(layout.width - piece.pieceWidth)
            val imageViewBottom = imageView.bottom
            val minTopMargin = imageViewBottom + SCATTER_TOP_MARGIN
            val maxTopMargin = layout.height - piece.pieceHeight
            lParams.topMargin = if (maxTopMargin > minTopMargin) {
                minTopMargin + Random().nextInt(maxTopMargin - minTopMargin)
            } else {
                minTopMargin
            }
            piece.layoutParams = lParams

            // Animate the piece
            val scaleXAnimator = ObjectAnimator.ofFloat(piece, "scaleX", 0f, 1f)
            val scaleYAnimator = ObjectAnimator.ofFloat(piece, "scaleY", 0f, 1f)
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(scaleXAnimator, scaleYAnimator)
            animatorSet.duration = SCATTER_ANIMATION_DURATION_MS
            animatorSet.startDelay = Random().nextInt(SCATTER_MAX_START_DELAY_MS).toLong() // 0-1500 ms
            animatorSet.interpolator = AccelerateDecelerateInterpolator()
            animatorSet.start()
        }
    }

    /**
     * Checks if the game is over (all puzzle pieces are in their correct positions).
     * If the game is over, it triggers the {@link PuzzleActivity#onGameOver()} method.
     * Otherwise, it plays a random "ok" sound.
     * @see PuzzleActivity#onGameOver()
     * @see SoundsPlayer
     */
    fun checkGameOver() {
        if (isGameOver()) {
            (activity as PuzzleActivity).onGameOver()
        } else {
            SoundsPlayer.play(okSoundsIds.random(), activity)
        }
    }

    /**
     * Determines if all puzzle pieces are in their final, unmovable positions.
     * @return True if the game is over, false otherwise.
     * @see PuzzlePiece#canMove
     */
    fun isGameOver(): Boolean {
        for (piece in pieces) {
            if (piece.canMove) {
                return false
            }
        }
        return true
    }

    /**
     * Plays a random win sound from the predefined list of win sound IDs.
     * @see SoundsPlayer
     */
    fun playWinSound() {
        SoundsPlayer.play(winSoundIds.random(), activity)
    }
}
