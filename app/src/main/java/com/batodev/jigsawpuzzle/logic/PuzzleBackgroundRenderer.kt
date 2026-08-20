package com.batodev.jigsawpuzzle.logic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import com.batodev.jigsawpuzzle.helpers.Settings
import com.caverock.androidsvg.SVG

/**
 * Renders the puzzle background (the source image and/or the puzzle-piece grid outline) onto an
 * [ImageView], according to the user's [Settings].
 */
object PuzzleBackgroundRenderer {
    private const val BACKGROUND_IMAGE_ALPHA = 70

    /**
     * Draws the puzzle background image and/or grid outline onto [imageView].
     * @param imageView The ImageView to render the background onto.
     * @param bitmap The source bitmap for the puzzle.
     * @param svgString The SVG string describing the puzzle piece grid.
     * @param settings The current app settings controlling what to show.
     */
    fun draw(
        imageView: ImageView,
        bitmap: Bitmap,
        svgString: String?,
        settings: Settings,
    ) {
        val bitmapCopy = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(bitmapCopy)
        val paint = Paint()
        paint.alpha = BACKGROUND_IMAGE_ALPHA
        if (settings.showImageInBackgroundOfThePuzzle) {
            canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint)
        }
        if (settings.showGridInBackgroundOfThePuzzle) {
            val svg = SVG.getFromString(svgString)
            svg.renderToCanvas(canvas)
        }
        imageView.setImageBitmap(bitmapCopy)
    }
}
