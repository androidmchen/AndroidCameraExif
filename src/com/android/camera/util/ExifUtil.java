/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.util;

import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.android.camera.exif.ExifInterface;
import com.android.camera.exif.ExifTag;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.TimeZone;

/**
 * Exif utility functions.
 */
public class ExifUtil {

    private static final String TAG = "CameraExif";

    /**
     * Adds the given location to the given exif.
     *
     * @param exif The exif to add the location tag to.
     * @param location The location to add.
     */
    public static void addLocationToExif(ExifInterface exif, Location location) {
        exif.addGpsTags(location.getLatitude(), location.getLongitude());
        exif.addGpsDateTimeStampTag(location.getTime());

        double altitude = location.getAltitude();
        if (altitude == 0) {
            return;
        }
        short altitudeRef =
                altitude < 0 ? ExifInterface.GpsAltitudeRef.SEA_LEVEL_NEGATIVE : ExifInterface.GpsAltitudeRef.SEA_LEVEL;
        exif.setTag(exif.buildTag(ExifInterface.TAG_GPS_ALTITUDE_REF, altitudeRef));
    }

    public static ExifInterface getExif(byte[] jpegData) {
        ExifInterface exif = new ExifInterface();
        try {
            exif.readExif(jpegData);
        } catch (IOException e) {
            Log.w(TAG, "Failed to read EXIF data", e);
        }
        return exif;
    }

    // Returns the degrees in clockwise. Values are 0, 90, 180, or 270.
    public static int getOrientation(ExifInterface exif) {
        Integer val = exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
        if (val == null) {
            return 0;
        } else {
            return ExifInterface.getRotationForOrientationValue(val.shortValue());
        }
    }

    public static int getRotationFromExif(String path) {
        return getRotationFromExifHelper(path, null, 0, null, null);
    }

    public static int getRotationFromExif(Context context, Uri uri) {
        return getRotationFromExifHelper(null, null, 0, context, uri);
    }

    public static int getRotationFromExif(Resources res, int resId) {
        return getRotationFromExifHelper(null, res, resId, null, null);
    }

    private static int getRotationFromExifHelper(String path, Resources res, int resId, Context context, Uri uri) {
        ExifInterface ei = new ExifInterface();
        try {
            if (path != null) {
                ei.readExif(path);
            } else if (uri != null) {
                InputStream is = context.getContentResolver().openInputStream(uri);
                BufferedInputStream bis = new BufferedInputStream(is);
                ei.readExif(bis);
            } else {
                InputStream is = res.openRawResource(resId);
                BufferedInputStream bis = new BufferedInputStream(is);
                ei.readExif(bis);
            }
            return getOrientation(ei);
        } catch (IOException e) {
            Log.w(TAG, "Getting exif data failed", e);
        }
        return 0;
    }

    public static int getOrientation(byte[] jpegData) {
        if (jpegData == null) {
            return 0;
        }
        ExifInterface exif = getExif(jpegData);
        return getOrientation(exif);
    }

    /**
     * Writes the JPEG data to a file. If there's EXIF info, the EXIF header will be added.
     *
     * @param path The path to the target file.
     * @param jpeg The JPEG data.
     * @param exif The EXIF info. Can be {@code null}.
     *
     * @return The size of the file. -1 if failed.
     */
    public static long writeFile(String path, byte[] jpeg, ExifInterface exif) {
        if (exif != null) {
            try {
                exif.writeExif(jpeg, path);
                File f = new File(path);
                return f.length();
            } catch (Exception e) {
                Log.e(TAG, "Failed to write data", e);
            }
        } else {
            return writeFile(path, jpeg);
        }
        return -1;
    }

    /**
     * Writes the data to a file.
     *
     * @param path The path to the target file.
     * @param data The data to save.
     *
     * @return The size of the file. -1 if failed.
     */
    private static long writeFile(String path, byte[] data) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
            return data.length;
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close file after write", e);
            }
        }
        return -1;
    }

    /**
     * Rotates the image by updating the exif. Done in background thread. The worst case is the whole file needed to be
     * re-written with modified exif data.
     */
    public static void rotateInJpegExif(String filePath, int rotationDegrees) {
        ExifInterface exifInterface = new ExifInterface();
        ExifTag tag =
                exifInterface.buildTag(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.getOrientationValueForRotation(rotationDegrees));
        if (tag != null) {
            exifInterface.setTag(tag);
            try {
                // Note: This only works if the file already has some EXIF.
                exifInterface.forceRewriteExif(filePath);
            } catch (FileNotFoundException e) {
                Log.w(TAG, "Cannot find file to set exif: " + filePath);
            } catch (IOException e) {
                Log.w(TAG, "Cannot set exif data: " + filePath);
            }
        } else {
            Log.w(TAG, "Cannot build tag: " + ExifInterface.TAG_ORIENTATION);
        }
    }

    /**
     * Adds basic EXIF data to the tiny planet image so it an be rewritten later.
     *
     * @param jpeg the JPEG data of the tiny planet.
     * @return The JPEG data containing basic EXIF.
     */
    public static byte[] addExif(byte[] jpeg) {
        ExifInterface exif = new ExifInterface();
        exif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME, System.currentTimeMillis(), TimeZone.getDefault());
        ByteArrayOutputStream jpegOut = new ByteArrayOutputStream();
        try {
            exif.writeExif(jpeg, jpegOut);
        } catch (IOException e) {
            Log.e(TAG, "Could not write EXIF", e);
        }
        return jpegOut.toByteArray();
    }
}
