package com.example.androidcameraexif;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.android.camera.exif.ExifInterface;
import com.android.camera.exif.ExifTag;
import com.android.camera.exif.Rational;
import com.android.camera.util.ExifUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Read tag
     * 
     * @param filePath
     */
    public static void readExifInfo(String filePath) {
        ExifInterface exif = new ExifInterface();
        try {
            exif.readExif(filePath);
            // exif.readExif(jpegData);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Could not find file to read exif: " + filePath, e);
        } catch (IOException e) {
            Log.w(TAG, "Could not read exif from file: " + filePath, e);
        }
        exif.getTag(ExifInterface.TAG_FLASH);
        exif.getTag(ExifInterface.TAG_IMAGE_WIDTH);
        exif.getTag(ExifInterface.TAG_IMAGE_LENGTH);
        exif.getTag(ExifInterface.TAG_MAKE);
        exif.getTag(ExifInterface.TAG_MODEL);
        exif.getTag(ExifInterface.TAG_APERTURE_VALUE);
        exif.getTag(ExifInterface.TAG_ISO_SPEED_RATINGS);
        exif.getTag(ExifInterface.TAG_WHITE_BALANCE);
        exif.getTag(ExifInterface.TAG_EXPOSURE_TIME);
        ExifTag focalTag = exif.getTag(ExifInterface.TAG_FOCAL_LENGTH);

        // Calculate the width and the height of the jpeg.
        Integer exifWidth = exif.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
        Integer exifHeight = exif.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);

        // Get image rotation from EXIF.
        int orientation = ExifUtil.getOrientation(exif);
        Integer tagval = exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
        List<ExifTag> taglist = exif.getAllTags();

        // write
        ExifTag directionRefTag =
                exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, ExifInterface.GpsTrackRef.MAGNETIC_DIRECTION);
        ExifTag directionTag = exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION, new Rational(0, 1));
        exif.setTag(directionRefTag);
        exif.setTag(directionTag);

        exif.setTag(exif.buildTag(ExifInterface.TAG_PIXEL_X_DIMENSION, exifWidth));
        exif.setTag(exif.buildTag(ExifInterface.TAG_PIXEL_Y_DIMENSION, exifHeight));

        exif.setTagValue(ExifInterface.TAG_PIXEL_X_DIMENSION, exifWidth);
        exif.setTagValue(ExifInterface.TAG_PIXEL_Y_DIMENSION, exifHeight);

        ExifTag tag = null;
        String value = null;
        int type = tag.getDataType();
        if (type == ExifTag.TYPE_UNSIGNED_RATIONAL || type == ExifTag.TYPE_RATIONAL) {
            value = String.valueOf(tag.getValueAsRational(0).toDouble());
        } else if (type == ExifTag.TYPE_ASCII) {
            value = tag.getValueAsString();
        } else {
            value = String.valueOf(tag.forceGetValueAsLong(0));
        }
    }

}
