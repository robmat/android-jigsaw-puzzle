package com.batodev.jigsawpuzzle.helpers

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Manages the "camera_images/temp.jpg" scratch file used to receive a photo
 * from the camera intent (or a copied gallery pick), and its FileProvider URI.
 */
object CameraFileHelper {
    private const val CAMERA_IMAGES_DIR = "camera_images"
    private const val TEMP_IMAGE_NAME = "temp.jpg"

    private fun tempImageFile(context: Context): File {
        val directory = File(context.filesDir, CAMERA_IMAGES_DIR)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File(directory, TEMP_IMAGE_NAME)
    }

    fun createPhotoUri(context: Context): Uri =
        FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".fileprovider",
            tempImageFile(context),
        )

    fun copyFdToTempFile(
        context: Context,
        fd: FileDescriptor,
    ): File {
        val destination = tempImageFile(context)
        FileInputStream(fd).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
        return destination
    }
}
