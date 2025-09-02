package com.batodev.jigsawpuzzle.model

import java.io.Serializable

data class GameState(
    val imageFileName: String?,
    val photoPath: String?,
    val puzzlesWidth: Int,
    val puzzlesHeight: Int,
    val elapsedTime: Int,
    val pieces: List<PieceState>,
    val svgString: String?
) : Serializable

data class PieceState(
    val xCoord: Int,
    val yCoord: Int,
    val currentX: Int,
    val currentY: Int,
    val pieceWidth: Int,
    val pieceHeight: Int,
    val canMove: Boolean,
    val imagePath: String
) : Serializable
