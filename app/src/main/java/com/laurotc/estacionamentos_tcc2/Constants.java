package com.laurotc.estacionamentos_tcc2;

/**
 * Created by laurotc on 9/8/16.
 */

import android.content.Context;
import android.content.res.Resources;
import com.google.android.gms.location.DetectedActivity;

import java.text.DecimalFormat;

/**
 * Constants used.
 */
public final class Constants {

    private Constants() {}

    /**************
     * Constant Values
     **************/

    public static final String PACKAGE_NAME = "com.laurotc.estacionamentos_tcc2";
    public static final String BROADCAST_ACTION = PACKAGE_NAME + ".BROADCAST_ACTION";
    public static final String ACTIVITY_EXTRA = PACKAGE_NAME + ".ACTIVITY_EXTRA";
    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES";
    public static final String ACTIVITY_UPDATES_REQUESTED_KEY = PACKAGE_NAME + ".ACTIVITY_UPDATES_REQUESTED";
    public static final String DETECTED_ACTIVITIES = PACKAGE_NAME + ".DETECTED_ACTIVITIES";

    //Desired time between activity detections. 0 to detect in the fastest possible rate
    public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 0;

    //List of activities
    protected static final int[] MONITORED_ACTIVITIES = {
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.ON_FOOT,
            DetectedActivity.STILL,
            DetectedActivity.UNKNOWN,
            DetectedActivity.TILTING
    };

    //Status
    public static final int PARKED = 1;
    public static final int NOT_PARKED = 0;

    //Server addr
    public static final String SERVER_ALIAS = "location";
    public static final String SERVER_ALIAS_TEST = "location_test";
    public static final String SERVER_ADDR_SEND_DATA = "http://ec.unisc.br/tcc2/?q="+Constants.SERVER_ALIAS;
    public static final String SERVER_ADDR_SEND_DATA_TEST = "http://ec.unisc.br/tcc2/?q="+Constants.SERVER_ALIAS_TEST;

    //Points to set the area to be considered
    public static final double UNISC_CENTER_LAT = -29.697982;
    public static final double UNISC_CENTER_LONG = -52.436860;

    public static final double NE_BOUND_LAT = -29.695981;
    public static final double NE_BOUND_LONG = -52.433506;

    public static final double SW_BOUND_LAT = -29.699606;
    public static final double SW_BOUND_LONG = -52.441435;

    public static final int UPDATE = 1;
    public static final int READ = 0;

    /****************
     * Util Methods
     ****************/

    /**
     * Returns a String of the detected activity type.
     */
    public static String getActivityString(Context context, int detectedActivityType)
    {
        Resources resources = context.getResources();
        switch(detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.in_vehicle);
            case DetectedActivity.ON_FOOT:
            case DetectedActivity.RUNNING:
            case DetectedActivity.WALKING:
                return resources.getString(R.string.on_foot);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still);
            case DetectedActivity.TILTING:
            case DetectedActivity.UNKNOWN:
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.tilting);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }
    }

    /**
     * Returns a String with the status to show on the Toast
     */
    public static String getActivityStringToast(Context context, int detectedActivityType)
    {

        Resources resources = context.getResources();
        switch(detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.in_vehicle_toast);
            case DetectedActivity.ON_BICYCLE:
            case DetectedActivity.ON_FOOT:
            case DetectedActivity.RUNNING:
            case DetectedActivity.WALKING:
            case DetectedActivity.TILTING:
                return resources.getString(R.string.walking_tilting_toast);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still_toast);
            case DetectedActivity.UNKNOWN:
                return resources.getString(R.string.unknown);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }
    }

    /**
     * Get string of device status
     */
    public static String getStatusString(int status)
    {
        return status == PARKED ? "PARKED" : "NOT PARKED";
    }

    /**
     * Converts speed in m/s to kmh
     */
    public static double speedInKmh(double speed)
    {
        double speedKmh = speed*3.6;
        DecimalFormat df = new DecimalFormat("#.##");
        return Double.valueOf(df.format(speedKmh));
    }

}

