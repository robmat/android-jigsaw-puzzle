package com.batodev.jigsawpuzzle.logic

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.activity.PuzzleActivity
import com.batodev.jigsawpuzzle.cut.PuzzleCurvesGenerator
import com.batodev.jigsawpuzzle.cut.PuzzleCutter
import com.batodev.jigsawpuzzle.helpers.Settings
import com.batodev.jigsawpuzzle.helpers.SoundsPlayer
import com.batodev.jigsawpuzzle.view.PuzzlePiece
import com.batodev.jigsawpuzzle.view.TouchListener
import com.caverock.androidsvg.SVG
import com.otaliastudios.zoom.ZoomLayout
import java.util.Random

/**
 * A class for managing the puzzle game logic.
 */
class PuzzleGameManager(
    private val activity: AppCompatActivity,
    private val layout: RelativeLayout,
    private val imageView: ImageView,
    private val zoomableLayout: ZoomLayout,
    private val settings: Settings,
    private val puzzleProgressListener: PuzzleProgressListener
) {
    var pieces: MutableList<PuzzlePiece> = mutableListOf()
    private val winSoundIds = listOf(
        R.raw.success_1, R.raw.success_2, R.raw.success_3, R.raw.success_4,
        R.raw.success_5, R.raw.success_6
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
        val svgString = puzzleCurvesGenerator.generateSvg()

        val bitmapCopy = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(bitmapCopy)
        val paint = Paint()
        paint.alpha = 70
        if (settings.showImageInBackgroundOfThePuzzle) {
            canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint)
        }
        if (settings.showGridInBackgroundOfThePuzzle) {
            val svg = SVG.getFromString(svgString)
            svg.renderToCanvas(canvas)
        }
        imageView.setImageBitmap(bitmapCopy)

        val pieceWidth = bitmap.width / puzzlesWidth
        val pieceHeight = bitmap.height / puzzlesHeight

        var yCoord = 0
        for (row in 0 until puzzlesHeight) {
            var xCoord = 0
            for (col in 0 until puzzlesWidth) {
                var offsetX = 0
                var offsetY = 0
                if (col > 0) {
                    offsetX = pieceWidth / 3
                }
                if (row > 0) {
                    offsetY = pieceHeight / 3
                }

                val piece = PuzzlePiece(activity)
                piece.xCoord = xCoord - offsetX + imageView.left + 4
                piece.yCoord = yCoord - offsetY + imageView.top + 7
                piece.pieceWidth = pieceWidth + offsetX
                piece.pieceHeight = pieceHeight + offsetY
                pieces.add(piece)
                xCoord += pieceWidth
            }
            yCoord += pieceHeight
        }
        PuzzleCutter.cut(bitmap, puzzlesHeight, puzzlesWidth, svgString, imageView, puzzleProgressListener, pieces)
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
            val minTopMargin = imageViewBottom + 10
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
            animatorSet.duration = 500 // milliseconds
            animatorSet.startDelay = Random().nextInt(1501).toLong() // Random delay between 0 and 1500 ms
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
    private fun isGameOver(): Boolean {
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
