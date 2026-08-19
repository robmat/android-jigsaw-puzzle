package com.batodev.jigsawpuzzle.helpers

import android.app.WallpaperManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Copies a gallery image out of the app's assets into a shareable temp file,
 * then builds a share [Intent] for it or sets it as the device wallpaper.
 */
object GalleryImageExporter {
    private const val COPY_BUFFER_SIZE = 10240

    fun copyAssetToTempFile(context: Context, assetPath: String): File {
        val stream = context.assets.open(assetPath)
        val dirShared = File(context.filesDir, "shared")
        if (!dirShared.exists()) {
            dirShared.mkdir()
        }
        val fileShared = File(dirShared, "shared.jpg")
        if (fileShared.exists()) {
            fileShared.delete()
        }
        fileShared.createNewFile()
        FileOutputStream(fileShared).use { output ->
            stream.use { input ->
                val buffer = ByteArray(COPY_BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytes -> bytesRead = bytes } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }
        }
        return fileShared
    }

    fun shareIntentFor(context: Context, fileShared: File): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND)
        val applicationId = context.applicationContext.packageName
        val uri = FileProvider.getUriForFile(context, "$applicationId.fileprovider", fileShared)
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.clipData = ClipData.newRawUri("", uri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareIntent.type = "image/*"
        return shareIntent
    }

    fun setAsWallpaper(context: Context, fileShared: File) {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val bitmap = BitmapFactory.decodeFile(fileShared.absolutePath)
        wallpaperManager.setBitmap(bitmap)
    }
}
