package com.batodev.jigsawpuzzle.logic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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

    fun scatterPieces() {
        val touchListener = TouchListener(this, zoomableLayout)
        pieces.shuffle()
        for (piece in pieces) {
            piece.setOnTouchListener(touchListener)
            layout.addView(piece)
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
        }
    }

    fun checkGameOver() {
        if (isGameOver()) {
            (activity as PuzzleActivity).onGameOver()
        } else {
            SoundsPlayer.play(okSoundsIds.random(), activity)
        }
    }

    private fun isGameOver(): Boolean {
        for (piece in pieces) {
            if (piece.canMove) {
                return false
            }
        }
        return true
    }

    fun playWinSound() {
        SoundsPlayer.play(winSoundIds.random(), activity)
    }
}
