package bachelorapp.fi.muni.cz.whitebalanceapp;


import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import bachelorapp.fi.muni.cz.whitebalanceapp.whiteBalance.algorithms.grayWorld.ConversionGW;
import bachelorapp.fi.muni.cz.whitebalanceapp.whiteBalance.algorithms.histogramStretching.HistogramStretching;
import bachelorapp.fi.muni.cz.whitebalanceapp.whiteBalance.algorithms.subsamplingWP.SubsamplingWB;
import bachelorapp.fi.muni.cz.whitebalanceapp.whiteBalance.algorithms.whitePatch.Conversion;
import bachelorapp.fi.muni.cz.whitebalanceapp.whiteBalance.partialConversions.PixelData;

import static android.graphics.Bitmap.createScaledBitmap;

/**
 * Created by Vladimira Hezelova on 25. 8. 2015.
 */
public class ConvertedPhotos extends AppCompatActivity {

    private Context instance;
    private final String TAG = "ConvertedPhotos";
    private ProgressBar bar;

    private ImageButton[] imageButtons;
    private ImageButton selectedImage;

    private String imagePath;

    private Bitmap originalBitmap;
    private int scaledHeight = 0;
    private int scaledWidth = 0;
    private Bitmap scaledBitmap;
    private Bitmap[] convertedBitmaps;
    // index pre ukladanie obrazku
    private int indexOfSelectedBitmap;

    private double[][] pixelDataOriginal;
    private double[][] pixelDataClone;

    private PixelData pixelDataInstance1;
    private PixelData pixelDataInstance2;
    private PixelData pixelDataInstance3;
    private PixelData pixelDataInstance4;

    private HistogramStretching histogramStretching;
    private ConversionGW conversionGW;
    private Conversion conversion;
    private SubsamplingWB subsamplingWB;

    private long start;
    private long end;

    // for coordinates for WhitePatch
    // enlarge of chosen pixel because of noise
    private int shiftedX;
    private int shiftedY;
    private int size = 9;
    private Bitmap selectedWhite;

    private ImageButton iconWP;
    private boolean setIconWP;
    private TextView textWP;

    final String PREFS_NAME = "MyPrefsFile";



    public ConvertedPhotos() {
        instance = this;
        Log.i(TAG,"constructor");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.converted_photos_layout);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.actionbar);

        MainActivity.mainActivity.finish();

        boolean writableExternalStorage = isExternalStorageWritable();
        Log.e("writableExternalStorage", Boolean.toString(writableExternalStorage));
        indexOfSelectedBitmap = 0;

        setIconWP = true;
        iconWP = (ImageButton) findViewById(R.id.icon_wp);
        Bitmap icon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.finger_icon);
        iconWP.setImageBitmap(icon);
        iconWP.setVisibility(View.GONE);
        textWP = (TextView) findViewById(R.id.text_wp);
        textWP.setVisibility(View.GONE);

        imageButtons = new ImageButton[]{
                (ImageButton) findViewById(R.id.original_image),
                (ImageButton) findViewById(R.id.converted_image1),
                (ImageButton) findViewById(R.id.converted_image2),
                (ImageButton) findViewById(R.id.converted_image3),
                (ImageButton) findViewById(R.id.converted_image4)
        };
        selectedImage = (ImageButton) findViewById(R.id.selected_image);

        bar = (ProgressBar) findViewById(R.id.progressBar);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        imagePath = intent.getStringExtra("imagePath");

        // free memory
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        Log.i(TAG, "free memory = " + Integer.toString(memoryClass));

        // BitmapFactory.Options opts=new BitmapFactory.Options();

        originalBitmap = BitmapFactory.decodeFile(imagePath);

        changeDimensions();
        scaledBitmap = createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, false);

        if(!originalBitmap.isRecycled() && !originalBitmap.sameAs(scaledBitmap)) {
            originalBitmap.recycle();
        }

        new ProgressTask().execute();
        /*
        //prekrytie layoutu navodom
        Intent intentTransparent = new Intent(getApplicationContext(), ConvertedPhotosTransparent.class);
        intentTransparent.putExtra("convertedBitmap1", convertedBitmap1);
        startActivity(intentTransparent);
        */
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_converted_photos, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_download:
                if(indexOfSelectedBitmap == 0) {
                    Toast.makeText(instance.getApplicationContext(), R.string.save_message1, Toast.LENGTH_SHORT).show();
                } else {
                    System.gc();
                    writeImage(indexOfSelectedBitmap);
                }
                return true;
            case android.R.id.home:
                if(!originalBitmap.isRecycled()) {
                    originalBitmap.recycle();
                }
                if(!scaledBitmap.isRecycled()) {
                    scaledBitmap.recycle();
                }
                for(int i = 0; i < convertedBitmaps.length; i++) {
                    if(convertedBitmaps[i] != null) {
                        if(!convertedBitmaps[i].isRecycled()) {
                            convertedBitmaps[i].recycle();
                        }
                    }
                }
                if((selectedWhite!= null) && (!selectedWhite.isRecycled())) {
                    selectedWhite.recycle();
                }
                finish();
                Intent mainActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainActivityIntent);
                /*
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    TaskStackBuilder.create(this)
                            .addNextIntentWithParentStack(upIntent)
                            .startActivities();
                } else {
                    NavUtils.navigateUpTo(this, upIntent);
                }
                */
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void writeImage(int indexOfSelectedBitmap) {
        /*
        String f = compressImage(imagePath);
        Log.e("ffffffffffff ", f);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                + Environment.getExternalStorageDirectory())));
                */
        /*
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            File file = new File(imagePath);
            in = new BufferedInputStream(new FileInputStream(file));
            Bitmap original = BitmapFactory.decodeStream(in);
            out = new ByteArrayOutputStream();
            Log.e("original before", Integer.toString(original.getRowBytes() * original.getHeight()));
            original.compress(Bitmap.CompressFormat.JPEG, 50, out);
            Log.e("original after", Integer.toString(original.getRowBytes() * original.getHeight()));
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            int memoryClass = am.getMemoryClass();
            Log.i(TAG, "free memory = " + Integer.toString(memoryClass));
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
            */
/*
            Date date = new Date();
            String sDate = new SimpleDateFormat("yyyyMMdd_hhmmss").format(date);

            String path = new File(imagePath).getParent();

            String fileNameWithExtension = new File(imagePath).getName();
            String fileName = sDate;
            String extension = "jpeg";
            int pos = fileNameWithExtension.lastIndexOf(".");
            if (pos > 0) {
                fileName = fileNameWithExtension.substring(0, pos);
                extension = fileNameWithExtension.substring(pos);
            }
            */
        if(convertedBitmaps[indexOfSelectedBitmap].sameAs(originalBitmap)) {
            Toast.makeText(instance.getApplicationContext(), R.string.save_message1, Toast.LENGTH_SHORT).show();
        } else {
            String destinationFilename = getFilename();
            Log.i("destinationFilename", destinationFilename);

            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));

                Log.e("out before", Integer.toString(convertedBitmaps[indexOfSelectedBitmap].getRowBytes() * convertedBitmaps[indexOfSelectedBitmap].getHeight()));
                convertedBitmaps[indexOfSelectedBitmap].compress(Bitmap.CompressFormat.JPEG, 50, bos);
                Log.e("out after", Integer.toString(convertedBitmaps[indexOfSelectedBitmap].getRowBytes() * convertedBitmaps[indexOfSelectedBitmap].getHeight()));
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                        + Environment.getExternalStorageDirectory())));
                Toast.makeText(instance.getApplicationContext(),R.string.save_message2, Toast.LENGTH_SHORT).show();
                //  convertedBitmaps[indexOfSelectedBitmap].recycle();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bos != null) {
                        bos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    private class ProgressTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute(){
            bar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            long start = System.currentTimeMillis();


            pixelDataOriginal = new double[scaledWidth*scaledHeight][3];

            pixelDataInstance1 = new PixelData();
            pixelDataInstance2 = new PixelData();
            pixelDataInstance4 = new PixelData();
            /*
            pixelDataOriginal = PixelData.getPixelData(scaledBitmap, pixelDataOriginal);
            pixelDataClone = new double[scaledWidth*scaledHeight][3];
            deepCopyPixelData();

*/

            histogramStretching = new HistogramStretching();
            conversionGW = new ConversionGW();
            subsamplingWB = new SubsamplingWB();

            convertedBitmaps = new Bitmap[] {
                    null,
                    histogramStretching.conversion(scaledWidth, scaledHeight, pixelDataInstance1.getPixelData(scaledBitmap, pixelDataOriginal)),
                    conversionGW.convert(scaledWidth, scaledHeight, pixelDataInstance2.getPixelData(scaledBitmap, pixelDataOriginal)),
                    null,
                    subsamplingWB.conversion(scaledWidth, scaledHeight, pixelDataInstance4.getPixelData(scaledBitmap, pixelDataOriginal))
            };



            end = System.currentTimeMillis();
            double time = (double) (end - start) / 1000;
            Log.i(TAG, "time of conversions = " + time + "seconds");
            //56.162 sec,56.188, 47.875, 45.121, 45.746, 45.529 .. 13.5
            // 35.594, 35.697
            //35 MB, 28MB, 35 MB .. 23 MB
            // 50,60 max 80%

            return null;
        }


        @Override
        protected void onPostExecute(Void result) {

            bar.setVisibility(View.GONE);

            setBitmapInButton(0, scaledBitmap);
            setBitmapInButton(1);
            setBitmapInButton(2);
            setBitmapInButton(3, scaledBitmap);
            setBitmapInButton(4);

            selectedImage.setImageBitmap(scaledBitmap);

            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            if(settings.getBoolean("my_first_time", true)) {
                //the app is being launched for first time, do something
                Log.d("Comments", "First time");

                Intent intentTransparent = new Intent(getApplicationContext(), ConvertedPhotosTransparent.class);
                startActivity(intentTransparent);

                // record the fact that the app has been started at least once
                   settings.edit().putBoolean("my_first_time", false).commit();
            }
        }
    }

    public void changeDimensions() {
        // dimensions of display
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int widthDisplay = size.x;
        int heightDisplay = size.y;
        int widthDisplayDp = pxToDp(widthDisplay);
        int heightDisplayDp = pxToDp(heightDisplay);

        Log.i(TAG, "display width in px: " + Integer.toString(widthDisplay));
        Log.i(TAG, "display height in px: " + Integer.toString(heightDisplay));
        Log.i(TAG, "display width in dp: " + Integer.toString(widthDisplayDp));
        Log.i(TAG, "display height in dp: " + Integer.toString(heightDisplayDp));

        int widthImage = originalBitmap.getWidth();
        int widthImageDp = pxToDp(widthImage);
        int heightImage = originalBitmap.getHeight();
        int heightImageDp = pxToDp(heightImage);


        Log.i(TAG, "bitmap width in px: " + Integer.toString(widthImage));
        Log.i(TAG, "bitmap height in px: " + Integer.toString(heightImage));
        Log.i(TAG, "bitmap width in dp: " + Integer.toString(widthImageDp));
        Log.i(TAG, "bitmap height in dp: " + Integer.toString(heightImageDp));


        //-200 nepada
        if(heightDisplay -400 >= heightImage && widthDisplay >= widthImage) {
            scaledHeight = heightImage;
            scaledWidth = widthImage;
        } else {
            scaledHeight = heightDisplay - 400;
            double ratio = (double)scaledHeight / (double)heightImage;
            scaledWidth = (int)((double)widthImage * ratio);
        }
        Log.i(TAG, "scaled width: " + Integer.toString(scaledWidth));
        Log.i(TAG, "scaled height: " + Integer.toString(scaledHeight));
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.ydpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.ydpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    public void setBitmapInButton(int index) {
        setBitmapInButton(index, convertedBitmaps[index]);
    }

    public void setBitmapInButton(final int index, final Bitmap bitmap) {
        imageButtons[index].setImageBitmap(bitmap);


        if(setIconWP == true && index == 3) {
            iconWP.setVisibility(View.VISIBLE);
            iconWP.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    indexOfSelectedBitmap = index;
                    selectedImage.setImageBitmap(bitmap);
                    if(setIconWP == true) {
                        textWP.setVisibility(View.VISIBLE);
                        if (index == 3) {
                            selectWhite();
                        }
                    }

                }
            });
        }
        imageButtons[index].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    indexOfSelectedBitmap = index;
                    selectedImage.setImageBitmap(bitmap);
                    if (index == 3) {
                        indexOfSelectedBitmap = index;
                        selectedImage.setImageBitmap(bitmap);
                        if (setIconWP == true) {
                            textWP.setVisibility(View.VISIBLE);
                            selectWhite();
                        }
                    } else {
                        selectedImage.setOnTouchListener(null);
                        textWP.setVisibility(View.GONE);
                    }
                }
            });


    }

    public void selectWhite() {
        selectedImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    iconWP.setVisibility(View.GONE);
                    textWP.setVisibility(View.GONE);
                    setIconWP = false;

                    // selected pixel of White
                    int selectedPixelX = (int) event.getX();
                    int selectedPixelY = (int) event.getY();

                    // kontrola zvacsenia vyberu bielych pixelov na okrajoch
                    if (selectedPixelX > 4) {
                        shiftedX = selectedPixelX - 4;
                    } else {
                        shiftedX = 1;
                    }
                    if (selectedPixelX > scaledBitmap.getWidth() - 5) {
                        shiftedX = scaledBitmap.getWidth() - 10;
                    }

                    if (selectedPixelY > 4) {
                        shiftedY = selectedPixelY - 4;
                    } else {
                        shiftedY = 1;
                    }
                    if (selectedPixelY > scaledBitmap.getHeight() - 5) {
                        shiftedY = scaledBitmap.getHeight() - 10;
                    }
                    selectedWhite = Bitmap.createBitmap(scaledBitmap, shiftedX, shiftedY, size, size);
                    selectedImage.setOnTouchListener(null);
                    new ProgressTask2().execute();
                    return true;
                } else
                    return false;
            }
        });
    }

    /*
    public void deepCopyPixelData() {
        for (int i = 0; i < pixelDataOriginal.length; i++) {
            pixelDataClone[i] = Arrays.copyOf(pixelDataOriginal[i], pixelDataOriginal[i].length);
        }
    }
    */

    private class ProgressTask2 extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            bar.setVisibility(View.VISIBLE);
            bar.bringToFront();
        }

        @Override
        protected Void doInBackground(Void... params) {
            pixelDataInstance3 = new PixelData();
            conversion = new Conversion();
            convertedBitmaps[3] = conversion.convert(scaledWidth, scaledHeight, selectedWhite, pixelDataInstance3.getPixelData(scaledBitmap, pixelDataOriginal));

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            bar.setVisibility(View.GONE);
            selectedImage.setImageBitmap(convertedBitmaps[3]);
            setBitmapInButton(3);
        }
    }

    public String compressImage(String filePath) {

        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

//      max Height and width values of the compressed image is taken as 816x612

        float maxHeight = 816.0f;
        float maxWidth = 612.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

//      width and height values are set maintaining the aspect ratio of the image

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

//      setting inSampleSize value allows to load a scaled down version of the original image

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {
//          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

//      check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out = null;
        String filename = getFilename();
        try {
            out = new FileOutputStream(filename);

//          write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return filename;

    }

    public String getFilename() {
        Date date = new Date();
        String sDate = new SimpleDateFormat("yyyyMMdd_hhmmss").format(date);

        String path = new File(imagePath).getParent();

        String fileNameWithExtension = new File(imagePath).getName();
        String fileName = sDate;
        String extension = "jpeg";
        int pos = fileNameWithExtension.lastIndexOf(".");
        if (pos > 0) {
            fileName = fileNameWithExtension.substring(0, pos);
            extension = fileNameWithExtension.substring(pos);
        }
        String destinationFilename = path + File.separatorChar + fileName + sDate + extension;
        Log.i("destinationFilename", destinationFilename);
        return destinationFilename;

    }

    private String getRealPathFromURI(String contentURI) {
        Uri contentUri = Uri.parse(contentURI);
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
/*
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }
*/
}

