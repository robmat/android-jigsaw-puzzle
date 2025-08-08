package com.batodev.jigsawpuzzle.logic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.ImageView
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.batodev.jigsawpuzzle.helpers.FirebaseHelper

/**
 * A class for loading and processing images for the puzzle.
 */
class ImageLoader(private val imageView: ImageView) {

    /**
     * Loads a bitmap from the application's assets and scales it to fit the ImageView.
     * @param assetName The name of the asset file (e.g., "image.png").
     * @param assets The AssetManager instance to access application assets.
     * @return The scaled and processed {@link Bitmap}.
     * @throws java.io.IOException if the asset file cannot be opened or read.
     */
    fun setPicFromAsset(assetName: String, assets: android.content.res.AssetManager): Bitmap {
        return try {
            val targetW = imageView.width
            val targetH = imageView.height
            val inputStream = assets.open("img/$assetName")
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            processBitmap(originalBitmap, targetW, targetH)
        } catch (e: Exception) {
            FirebaseHelper.logException(imageView.context, "setPicFromAsset", e.message)
            throw e
        }
    }

    /**
     * Loads a bitmap from a given file path and scales it to fit the ImageView.
     * Corrects the orientation of the bitmap based on EXIF data.
     * @param path The absolute path to the image file.
     * @return The scaled and processed {@link Bitmap}.
     * @throws java.io.IOException if the file cannot be opened or read.
     */
    fun setPicFromPath(path: String): Bitmap {
        return try {
            val targetW = imageView.width
            val targetH = imageView.height
            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, bmOptions)
            val photoW = bmOptions.outWidth
            val photoH = bmOptions.outHeight
            val scaleFactor = (photoW / targetW).coerceAtMost(photoH / targetH).coerceAtLeast(1)
            bmOptions.inJustDecodeBounds = false
            bmOptions.inSampleSize = scaleFactor
            val bitmap = BitmapFactory.decodeFile(path, bmOptions)
            val rotatedBitmap = uprightBitmap(bitmap, path)
            processBitmap(rotatedBitmap, targetW, targetH)
        } catch (e: Exception) {
            FirebaseHelper.logException(imageView.context, "setPicFromPath", e.message)
            throw e
        }
    }

    /**
     * Processes a bitmap by cropping and scaling it to fit the target dimensions.
     * Maintains the aspect ratio by cropping either width or height.
     * @param bitmap The original {@link Bitmap} to process.
     * @param targetW The target width for the bitmap.
     * @param targetH The target height for the bitmap.
     * @return The cropped and scaled {@link Bitmap}.
     */
    private fun processBitmap(bitmap: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val origW = bitmap.width
        val origH = bitmap.height
        val targetRatio = targetW.toFloat() / targetH.toFloat()
        val origRatio = origW.toFloat() / origH.toFloat()
        var cropW = origW
        var cropH = origH
        var cropX = 0
        var cropY = 0
        if (origRatio > targetRatio) {
            cropW = (origH * targetRatio).toInt()
            cropX = (origW - cropW) / 2
        } else if (origRatio < targetRatio) {
            cropH = (origW / targetRatio).toInt()
            cropY = (origH - cropH) / 2
        }
        cropW = cropW.coerceAtMost(origW)
        cropH = cropH.coerceAtMost(origH)
        cropX = cropX.coerceAtLeast(0)
        cropY = cropY.coerceAtLeast(0)

        val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
        return croppedBitmap.scale(targetW, targetH)
    }

    /**
     * Corrects the orientation of a bitmap based on its EXIF orientation tag.
     * @param bitmap The {@link Bitmap} to correct.
     * @param imagePath The path to the image file, used to read EXIF data.
     * @return The upright {@link Bitmap}.
     */
    private fun uprightBitmap(bitmap: Bitmap, imagePath: String): Bitmap {
        val exifInterface = ExifInterface(imagePath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
