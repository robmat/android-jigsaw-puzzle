package com.batodev.jigsawpuzzle.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import com.batodev.jigsawpuzzle.R
import com.batodev.jigsawpuzzle.helpers.SettingsHelper.load
import java.io.IOException

/**
 * An adapter for displaying images in a grid view.
 */
/**
 * An adapter for displaying images in a grid view.
 * @param mContext The context of the application.
 */
class ImageAdapter(private val mContext: Context) : BaseAdapter() {
    private val am: AssetManager = mContext.assets
    private var files: Array<String>? = null
    private val handler = Handler(Looper.getMainLooper())

    init {
        /**
         * Initializes the adapter by listing image files from the "img" asset folder.
         * If an IOException occurs, it is printed to the stack trace.
         */
        try {
            files = am.list("img")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Returns the number of items in the adapter.
     * @return The total count of image files.
     */
    override fun getCount(): Int {
        return files!!.size
    }

    /**
     * Returns the data item associated with the specified position in the data set.
     * This adapter does not directly return data items, so it always returns null.
     * @param position Position of the item whose data we want within the adapter's data set.
     * @return The data at the specified position.
     */
    override fun getItem(position: Int): Any? {
        return null
    }

    /**
     * Returns the row id associated with the specified position in the list.
     * This adapter does not use stable IDs, so it always returns 0.
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    override fun getItemId(position: Int): Long {
        return 0
    }

    /**
     * Get a View that displays the data at the specified position in the data set.
     * This method inflates a new view or recycles an existing one, then populates it with image data.
     * It also applies an alpha filter if the image is not yet uncovered in the game settings.
     * @param position The position of the item within the adapter's data set of the item whose view we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *        is non-null and of an appropriate type before using. If it is not possible to convert
     *        this view to display the correct data, this method can create a new view.
     * @param parent The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     * @throws RuntimeException if an IOException occurs while loading the image from assets.
     */
    @SuppressLint("InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val settings = load(mContext)
        var convertViewVar = convertView
        if (convertViewVar == null) {
            val layoutInflater = LayoutInflater.from(mContext)
            convertViewVar = layoutInflater.inflate(R.layout.grid_element, null)
        }
        val imageView = convertViewVar!!.findViewById<ImageView>(R.id.gridImageview)
        imageView.setImageBitmap(null)
        // run image related code after the view was laid out
        imageView.post {
            handler.post {
                try {
                    val picFromAsset = getPicFromAsset(imageView.height, imageView.width, files!![position], am)!!
                    val mutableBitmap = createBitmap(picFromAsset.width, picFromAsset.height)
                    val canvas = Canvas(mutableBitmap)
                    val alphaPaint = Paint()
                    if (!settings.uncoveredPics.contains(files!![position])) {
                        alphaPaint.alpha = 30
                    }
                    // Draw the original bitmap onto the canvas with the alpha paint
                    canvas.drawBitmap(picFromAsset, 0f, 0f, alphaPaint)
                    imageView.setImageBitmap(mutableBitmap)
                } catch (e: IOException) {
                    Log.w(ImageAdapter::class.java.simpleName, e.localizedMessage!!)
                    throw RuntimeException(e)
                }
            }
        }
        return convertViewVar
    }

    companion object {
        /**
         * Loads a bitmap from the application's assets, scaled to fit target dimensions.
         * @param targetH The target height for the bitmap.
         * @param targetW The target width for the bitmap.
         * @param assetName The name of the asset file (e.g., "image.png").
         * @param am The AssetManager instance to access application assets.
         * @return The scaled {@link Bitmap}, or null if target dimensions are zero.
         * @throws IOException if the asset file cannot be opened or read.
         */
        @Throws(IOException::class)
        fun getPicFromAsset(
            targetH: Int,
            targetW: Int,
            assetName: String,
            am: AssetManager,
        ): Bitmap? {
            if (targetW == 0 || targetH == 0) {
                // view has no dimensions set
                return null
            }
            val `is` = am.open("img/$assetName")
            // Get the dimensions of the bitmap
            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true
            BitmapFactory.decodeStream(`is`, Rect(-1, -1, -1, -1), bmOptions)
            val photoW = bmOptions.outWidth
            val photoH = bmOptions.outHeight

            // Determine how much to scale down the image
            val scaleFactor = (photoW / targetW).coerceAtMost(photoH / targetH)
            `is`.reset()

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false
            bmOptions.inSampleSize = scaleFactor
            return BitmapFactory.decodeStream(`is`, Rect(-1, -1, -1, -1), bmOptions)
        }
    }
}
