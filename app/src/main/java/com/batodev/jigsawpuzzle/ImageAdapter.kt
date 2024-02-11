package com.batodev.jigsawpuzzle

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
import com.batodev.jigsawpuzzle.SettingsHelper.load
import java.io.IOException

class ImageAdapter(private val mContext: Context) : BaseAdapter() {
    private val am: AssetManager = mContext.assets
    private var files: Array<String>? = null
    private val handler = Handler(Looper.getMainLooper())

    init {
        try {
            files = am.list("img")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getCount(): Int {
        return files!!.size
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    // create a new ImageView for each item referenced by the Adapter
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
                    val mutableBitmap = Bitmap.createBitmap(
                        picFromAsset.width, picFromAsset.height, Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(mutableBitmap)
                    val alphaPaint = Paint()
                    if (!settings.uncoveredPics.contains(files!![position])) {
                        alphaPaint.alpha = 30
                    }
                    // Draw the original bitmap onto the canvas with the alpha paint
                    canvas.drawBitmap(picFromAsset, 0f, 0f, alphaPaint)
                    imageView.setImageBitmap(mutableBitmap)
                } catch (e: IOException) {
                    Log.w(ImageAdapter::class.java.simpleName, e.localizedMessage)
                    throw RuntimeException(e)
                }
            }
        }
        return convertViewVar
    }

    companion object {
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
            val scaleFactor = Math.min(photoW / targetW, photoH / targetH)
            `is`.reset()

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false
            bmOptions.inSampleSize = scaleFactor
            return BitmapFactory.decodeStream(`is`, Rect(-1, -1, -1, -1), bmOptions)
        }
    }
}