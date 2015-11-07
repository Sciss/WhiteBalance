package bachelorapp.fi.muni.cz.whitebalanceapp;


import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import bachelorapp.fi.muni.cz.whitebalanceapp.whiteBalance.algorithms.grayWorld.ConversionGW;
import bachelorapp.fi.muni.cz.whitebalanceapp.whiteBalance.algorithms.histogramStretching.HistogramStretching;

import static android.graphics.Bitmap.createScaledBitmap;

/**
 * Created by Vladimira Hezelova on 25. 8. 2015.
 */
public class ConvertedPhotos extends ActionBarActivity {

    private static ImageButton originalImage;
    private static ImageButton convertedImage1;
    private static ImageButton convertedImage2;
    private static ImageButton convertedImage3;
    private static ImageButton convertedImage4;
    private static ImageButton convertedImage5;
    private static ImageButton selectedImage;
    private static Bitmap selectedBitmap;
    private static Context instance;

    private static String picturePath;

    private ProgressBar bar;

    private Bitmap convertedBitmap1;
    private Bitmap convertedBitmap2;
    private Bitmap convertedBitmap3;
    private Bitmap convertedBitmap4;
    private Bitmap bitmap;
    private Bitmap originalBitmap;
    private Bitmap originalBitmapTMP;

    private int height;
    private int width;

    private Bitmap histogramStretchedBitmap;


    // private String picturePath;


    public ConvertedPhotos() {
        instance = this;
       // convertedPhoto = (ImageView) findViewById(R.id.converted_photo);
        Log.e("log","ConvertedPhotosFragment");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.converted_photos_layout);


        bar = (ProgressBar) findViewById(R.id.progressBar);
        Log.e("log","create2");

        ActionBar actionBar = getSupportActionBar();
        /*
        actionBar.setLogo(R.drawable.icon_settings);
*/
        if (actionBar != null) {
            actionBar.setDisplayUseLogoEnabled(true);
        }
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
       // final String picturePath = intent.getStringExtra("picturePath");
        picturePath = intent.getStringExtra("picturePath");
        selectedImage = (ImageButton) findViewById(R.id.selected_image);

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        Log.e("free memory", Integer.toString(memoryClass));

        originalBitmapTMP = BitmapFactory.decodeFile(picturePath);
        // rozmery displayu
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int widthDisplay = size.x;
        int heightDisplay = size.y;

        Log.e("display width: ", Integer.toString(widthDisplay));
        Log.e("display height: ", Integer.toString(heightDisplay));

        int widthImage = originalBitmapTMP.getWidth();
        int heightImage = originalBitmapTMP.getHeight();
        Log.e("bitmap width: ", Integer.toString(widthImage));
        Log.e("bitmap height: ", Integer.toString(heightImage));


        height = heightDisplay - 380;
        Log.e("heightDisplay - 380", Double.toString(height));
        double ratio = (double)height / (double)heightImage;
        Log.e("ratio", Double.toString(ratio));
        width = (int)((double)widthImage * ratio);
        Log.e("width", Integer.toString(width));



       // BitmapFactory.Options bmOptions = new BitmapFactory.Options();
     //   Bitmap bitmapTMP = BitmapFactory.decodeFile(picturePath, bmOptions);
        bitmap = createScaledBitmap(originalBitmapTMP, width, height, false);

       // histogramStretchedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);


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
                writeImage(selectedBitmap);
                return true;
            case android.R.id.home:
                originalBitmapTMP = null;
                finish();
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                                    // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
/*
    public void openSettings() {

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("picturePath", picturePath);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
*/

    public static void writeImage(Bitmap image) {
        Date date = new Date();
        String sDate = new SimpleDateFormat("yyyyMMdd_hhmmss").format(date);

        Log.e("time", sDate);
        String destinationFilename = android.os.Environment.getExternalStorageDirectory().getPath()+ File.separatorChar +
                "DCIM" + File.separatorChar + "resources" + File.separatorChar + sDate + ".jpeg";

        Log.e("destinationFilename", destinationFilename);

        BufferedOutputStream bos = null;

        try {
            bos = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));
            image.compress(Bitmap.CompressFormat.PNG, 100, bos);
            Toast.makeText(instance.getApplicationContext(), "Saved succesfully", Toast.LENGTH_SHORT).show();
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

    private class ProgressTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute(){
            bar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            convertedBitmap1 = HistogramStretching.conversion(bitmap);
            convertedBitmap2 = ConversionGW.convert(bitmap);
            convertedBitmap4 = SubsamplingWB.conversion(bitmap);


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            bar.setVisibility(View.GONE);

            originalBitmap = bitmap;
            originalImage = (ImageButton) findViewById(R.id.original_image);
            originalImage.setImageBitmap(originalBitmap);

            selectedImage.setImageBitmap(originalBitmap);
            originalImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedImage.setImageBitmap(originalBitmap);
                }
            });

            convertedImage1 = (ImageButton) findViewById(R.id.converted_image1);
            convertedImage1.setImageBitmap(convertedBitmap1);

            convertedImage1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedImage.setImageBitmap(convertedBitmap1);
                }
            });

            convertedImage2 = (ImageButton) findViewById(R.id.converted_image2);
            convertedImage2.setImageBitmap(convertedBitmap2);

            convertedImage2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedImage.setImageBitmap(convertedBitmap2);

                }
            });

            convertedImage4 = (ImageButton) findViewById(R.id.converted_image4);
            convertedImage4.setImageBitmap(convertedBitmap4);

            convertedImage4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedImage.setImageBitmap(convertedBitmap4);
                }
            });

            selectedImage.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        // pozicia View (image) vzhladom na Activity (bez menu)
                      /*  int selectedImageX = (int) selectedImage.getX();
                        int selectedImageY = (int) selectedImage.getY();
                        Log.e("selectedImageX", Integer.toString(selectedImageX));
                        Log.e("selectedImageY", Integer.toString(selectedImageY));*/
                        // rozmery View (image)
                        int selectedImageWidth = (int) selectedImage.getWidth();
                        int selectedImageHeight = (int) selectedImage.getHeight();
                        Log.e("selectedImageWidth" , Integer.toString(selectedImageWidth));
                        Log.e("selectedImageHeight", Integer.toString(selectedImageHeight));
                        // selected pixel of White
                        int selectedPixelXInWindow = (int) event.getRawX();
                        int selectedPixelYInWindow = (int) event.getRawY();

                        // pozicia vybraneho pixelu vzhladom na celu obrazovku (aj s menu)
                        Log.e("selectedPixelX raw" , Integer.toString(selectedPixelXInWindow));
                        Log.e("selectedPixelY raw", Integer.toString(selectedPixelYInWindow));

                        int loacationViewInWindow[] = new int[2];
                        selectedImage.getLocationInWindow(loacationViewInWindow);
                        Log.e("test1[0]", Integer.toString(loacationViewInWindow[0]));
                        Log.e("test1[1]", Integer.toString(loacationViewInWindow[1]));

                        int selectedPixelX = selectedPixelXInWindow - loacationViewInWindow[0];
                        int selectedPixelY = selectedPixelYInWindow - loacationViewInWindow[1];
                        Log.e("selectedPixelX in View", Integer.toString(selectedPixelX));
                        Log.e("selectedPixelY in View", Integer.toString(selectedPixelY));


                        // enlarge of chosen pixel because of noise
                        int shiftedX;
                        int shiftedY;
                       // int size = 9;
                        int size = 9;
                        // kontrola zvacsenia vyberu bielych pixelov na okrajoch
                        if(selectedPixelX > 4) {
                            shiftedX = selectedPixelX - 4;
                        } else {
                            shiftedX = 1;
                        }
                        if(selectedPixelX > bitmap.getWidth() - 5) {
                            shiftedX = bitmap.getWidth() - 10;
                        }

                        if(selectedPixelY > 4) {
                            shiftedY = selectedPixelY - 4;
                        } else {
                            shiftedY = 1;
                        }
                        if(selectedPixelY > bitmap.getHeight() - 5) {
                            shiftedY = bitmap.getHeight() - 10;
                        }

                      //  shiftedX = 363;
                      //  shiftedY = 74;
                        try{
                            Bitmap selectedWhite = Bitmap.createBitmap(bitmap, shiftedX, shiftedY, size, size);

                            Log.e("selectedPixelX", Integer.toString(selectedPixelX));
                            Log.e("selectedPixelY", Integer.toString(selectedPixelY));
                            Log.e("shiftedX", Integer.toString(shiftedX));
                            Log.e("shiftedY", Integer.toString(shiftedY));
                            writeImage(selectedWhite);
                        } catch(IllegalArgumentException e) {
                            e.printStackTrace();
                        }

                   /*     convertedBitmap3 = Conversion.convert(bitmap, selectedWhite);
                        convertedImage3 = (ImageButton) findViewById(R.id.converted_image3);
                        convertedImage3.setImageBitmap(convertedBitmap3);

                        convertedImage3.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                selectedImage.setImageBitmap(convertedBitmap3);
                            }
                        });
*/
                        return true;

                    } else
                        return false;
                }
            });

        }
    }
}