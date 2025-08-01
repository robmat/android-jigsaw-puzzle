package com.batodev.jigsawpuzzle.view

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView

/**
 * A custom view for a puzzle piece.
 * This class extends {@link AppCompatImageView} and adds properties specific to a puzzle piece,
 * such as its correct coordinates, dimensions, and movability.
 * @param context The context in which the view is running.
 */
class PuzzlePiece(context: Context?) : AppCompatImageView(context!!) {
    /**
     * The correct X-coordinate of the puzzle piece on the game board.
     */
    var xCoord = 0
    /**
     * The correct Y-coordinate of the puzzle piece on the game board.
     */
    var yCoord = 0
    /**
     * The width of the puzzle piece.
     */
    var pieceWidth = 0
    /**
     * The height of the puzzle piece.
     */
    var pieceHeight = 0
    /**
     * A boolean indicating whether the puzzle piece can still be moved by the user.
     * Set to `false` once the piece is in its correct position.
     */
    var canMove = true
}
