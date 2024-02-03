package dragosholban.com.androidpuzzlegame;

import static dragosholban.com.androidpuzzlegame.ImageAdapter.getPicFromAsset;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class ImageAdapter extends BaseAdapter {
    private final Context mContext;
    private final AssetManager am;
    private String[] files;

    public ImageAdapter(Context c) {
        mContext = c;
        am = mContext.getAssets();
        try {
            files  = am.list("img");
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
        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.grid_element, null);
        }

        final ImageView imageView = convertView.findViewById(R.id.gridImageview);
        imageView.setImageBitmap(null);
        // run image related code after the view was laid out
        imageView.post(() -> {
            LoadImageAsyncTask asyncTask = new LoadImageAsyncTask(
                    imageView.getHeight(),
                    imageView.getWidth(),
                    am,
                    position,
                    files,
                    imageView::setImageBitmap);
            asyncTask.execute();
        });

        return convertView;
    }

    static Bitmap getPicFromAsset(int targetH, int targetW, String assetName, AssetManager am) {
        if(targetW == 0 || targetH == 0) {
            // view has no dimensions set
            return null;
        }

        try {
            InputStream is = am.open("img/" + assetName);
            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

            is.reset();

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            return BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }
}
class LoadImageAsyncTask extends AsyncTask<Void, Void, Void> {
    int targetW;
    int targetH;
    AssetManager am;
    int position;
    String[] files;
    Bitmap bitmap;
    Consumer<Bitmap> consumer;

    public LoadImageAsyncTask(int targetW, int targetH, AssetManager am, int position, String[] files, Consumer<Bitmap> consumer) {
        this.targetW = targetW;
        this.targetH = targetH;
        this.am = am;
        this.position = position;
        this.files = files;
        this.consumer = consumer;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        bitmap = getPicFromAsset(targetH, targetW, files[position], am);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        consumer.accept(bitmap);
    }
}