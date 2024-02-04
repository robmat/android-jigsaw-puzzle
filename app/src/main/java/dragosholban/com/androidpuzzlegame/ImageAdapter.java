package dragosholban.com.androidpuzzlegame;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

public class ImageAdapter extends BaseAdapter {
    private final Context mContext;
    private final AssetManager am;
    private String[] files;
    private final Handler handler = new Handler();

    public ImageAdapter(Context c) {
        mContext = c;
        am = mContext.getAssets();
        try {
            files = am.list("img");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCount() {
        return files.length;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    @SuppressLint("InflateParams")
    public View getView(final int position, View convertView, ViewGroup parent) {
        Settings settings = SettingsHelper.INSTANCE.load(mContext);
        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.grid_element, null);
        }

        final ImageView imageView = convertView.findViewById(R.id.gridImageview);
        imageView.setImageBitmap(null);
        // run image related code after the view was laid out
        imageView.post(() -> handler.post(() -> {
            try {
                Bitmap picFromAsset = getPicFromAsset(imageView.getHeight(), imageView.getWidth(), files[position], am);
                assert picFromAsset != null;
                Bitmap mutableBitmap = Bitmap.createBitmap(picFromAsset.getWidth(), picFromAsset.getHeight(), Bitmap.Config.ARGB_8888);
                // Create a canvas to draw on the mutable bitmap
                Canvas canvas = new Canvas(mutableBitmap);
                // Create a paint with the desired alpha value
                Paint alphaPaint = new Paint();
                if (!settings.getUncoveredPics().contains(files[position])) {
                    alphaPaint.setAlpha(30);
                }
                // Draw the original bitmap onto the canvas with the alpha paint
                canvas.drawBitmap(picFromAsset, 0, 0, alphaPaint);
                imageView.setImageBitmap(mutableBitmap);
            } catch (IOException e) {
                Log.w(ImageAdapter.class.getSimpleName(), e.getLocalizedMessage());
                throw new RuntimeException(e);
            }
        }));
        return convertView;
    }

    static Bitmap getPicFromAsset(int targetH, int targetW, String assetName, AssetManager am) throws IOException {
        if (targetW == 0 || targetH == 0) {
            // view has no dimensions set
            return null;
        }

        InputStream is = am.open("img/" + assetName);
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        is.reset();

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
    }
}