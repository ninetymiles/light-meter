package com.rex.lightmeter;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Surface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Helper class for lock/unlock orientation
 */
public class OrientationHelper {

    private final Logger mLogger = LoggerFactory.getLogger("RexLog");

    private final Activity mActivity;

    // Copied from Android docs, since we don't have these values in Froyo 2.2
    private final int SCREEN_ORIENTATION_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    private final int SCREEN_ORIENTATION_REVERSE_LANDSCAPE = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) ?
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    private final int SCREEN_ORIENTATION_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    private final int SCREEN_ORIENTATION_REVERSE_PORTRAIT = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) ?
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT :
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

    public OrientationHelper(Activity activity) {
        mActivity = activity;
    }

    public void lock() {
        mLogger.trace("");
        final int orientation = mActivity.getResources().getConfiguration().orientation;
        final int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int value = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;

        switch (orientation) {
        case Configuration.ORIENTATION_PORTRAIT:
            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_180) {
                value = SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            } else {
                value = SCREEN_ORIENTATION_PORTRAIT;
            }
            break;
        case Configuration.ORIENTATION_LANDSCAPE:
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                value = SCREEN_ORIENTATION_LANDSCAPE;
            } else {
                value = SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            }
            break;
        }

        mActivity.setRequestedOrientation(value);
    }

    public void unlock() {
        mLogger.trace("");
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}
