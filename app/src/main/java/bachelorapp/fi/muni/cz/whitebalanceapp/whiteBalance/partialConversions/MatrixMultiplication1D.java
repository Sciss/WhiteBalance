package bachelorapp.fi.muni.cz.whitebalanceapp.whiteBalance.partialConversions;

/**
 * Created by Vladimira Hezelova on 5. 12. 2015.
 */
public class MatrixMultiplication1D {
    // matrix for conversion from normalized, linearized RGB to CIE D65 XYZ
    public static final float[][] MATRIX_RGBtoXYZ =
            {{0.4124f, 0.3576f, 0.1805f},
                    {0.2126f, 0.7152f, 0.0722f},
                    {0.0193f, 0.1192f, 0.9505f}};

    // matrix for conversion from CIE D65 XYZ to LMS (tristimulus values of the Long, Medium, Short)
    public static final float[][] MATRIX_XYZtoLMS =
            {{0.7982f, 0.3389f, -0.1371f},
                    {-0.5918f, 1.5512f, 0.0406f},
                    {0.0008f, 0.0239f, 0.9753f}};

    // inverse matrix to XYZtoLMS
    // matrix for conversion from LMS to CIE D65 XYZ
    public static final float[][] MATRIX_LMStoXYZ =
            {{1.07645f,-0.23766f, 0.16121f},
                    {0.41096f, 0.55434f, 0.03469f},
                    {-0.01095f, -0.01338f, 1.02434f}};

    // inverse matrix to RGBtoXYZ
    // matrix for conversion from CIE D65 XYZ to RGB
    public static final float[][] MATRIX_XYZtoRGB =
            {{3.24062f, -1.53720f, -0.49862f},
                    {-0.96893f, 1.87575f, 0.04151f},
                    {0.05571f, -0.20402f, 1.05699f}};

    private float[] array = new float[3];

    /**
     * |x| |a b c||u|
     * |y|=|d e f||v|
     * |z| |g h i||w|
     * Z dvojrozmerneho pola uvw vsetky hodnoty prekonvertuje vynasobenim  matrix maticou 3x3
     * a vrati ziskane dvojrozmerne pole. Pozicie v poli sa zachovavaju
     * pre informaciu pozicie pixelu v obraze(prvy rozmer) a zlozku RGB(druhy rozmer)
     *
     */
    public float[] multiply(float[][] matrix, float[] uvw) {

        array[0] = matrix[0][0] * uvw[0] + matrix[0][1] * uvw[1] + matrix[0][2] * uvw[2];
        array[1] = matrix[1][0] * uvw[0] + matrix[1][1] * uvw[1] + matrix[1][2] * uvw[2];
        array[2] = matrix[2][0] * uvw[0] + matrix[2][1] * uvw[1] + matrix[2][2] * uvw[2];

        return array;
    }

    public float[] multiply(float[][] matrix, float[] uvw, float[] outRGB) {

        outRGB[0] = matrix[0][0] * uvw[0] + matrix[0][1] * uvw[1] + matrix[0][2] * uvw[2];
        outRGB[1] = matrix[1][0] * uvw[0] + matrix[1][1] * uvw[1] + matrix[1][2] * uvw[2];
        outRGB[2] = matrix[2][0] * uvw[0] + matrix[2][1] * uvw[1] + matrix[2][2] * uvw[2];

        return outRGB;
    }



}
