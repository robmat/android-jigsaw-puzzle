package com.batodev.jigsawpuzzle.logic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.ImageView
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface

/**
 * A class for loading and processing images for the puzzle.
 */
class ImageLoader(private val imageView: ImageView) {

    fun setPicFromAsset(assetName: String, assets: android.content.res.AssetManager): Bitmap {
        val targetW = imageView.width
        val targetH = imageView.height
        val inputStream = assets.open("img/$assetName")
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return processBitmap(originalBitmap, targetW, targetH)
    }

    fun setPicFromPath(path: String): Bitmap {
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
        return processBitmap(rotatedBitmap, targetW, targetH)
    }

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
