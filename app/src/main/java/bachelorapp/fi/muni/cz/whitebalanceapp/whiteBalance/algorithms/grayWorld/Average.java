package bachelorapp.fi.muni.cz.whitebalanceapp.whiteBalance.algorithms.grayWorld;

import android.graphics.Bitmap;

/**
 * Created by Vladimira Hezelova on 25. 4. 2015.
 */
public class Average {

    private float avgR;
    private float avgG;
    private float avgB;
    private float avgGray;
    private float kR;
    private float kG;
    private float kB;

    private Bitmap bitmap;
    private int width;
    private int height;

    public Average(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
    }

    public float[][] getScalingMatrix() {
        findAverages();
        findAvgGray();
        findScalingCoefficients();

        float[][] scalingMatrix = new float[][]{{kR, 0.0f, 0.0f}, {0.0f, kG, 0.0f}, {0.0f, 0.0f, kB}};
        return scalingMatrix;
    }

    private void findAverages() {
        float sumR = 0;
        float sumG = 0;
        float sumB = 0;
        int value;

        for(int i = 0; i < height; i++) {
            for(int j = 0; j < width; j++) {
                value = bitmap.getPixel(j,i);
                sumR += (value >> 16) & 0xff; //red;
                sumG += (value >>  8) & 0xff; //green
                sumB += (value      ) & 0xff;  //blue
            }
        }
        float numberOfPixels = height*width;
        avgR = sumR / numberOfPixels;
        avgG = sumG / numberOfPixels;
        avgB = sumB / numberOfPixels;
    }

    private void findAvgGray() {
        avgGray = 0.299f * avgR + 0.587f * avgG + 0.114f * avgB;
    }

    private void findScalingCoefficients() {
        kR = avgGray / avgR;
        kG = avgGray / avgG;
        kB = avgGray / avgB;
    }
}
