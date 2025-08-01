package com.batodev.jigsawpuzzle.view

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView

/**
 * A custom view for a puzzle piece.
 */
class PuzzlePiece(context: Context?) : AppCompatImageView(context!!) {
    var xCoord = 0
    var yCoord = 0
    var pieceWidth = 0
    var pieceHeight = 0
    var canMove = true
}
