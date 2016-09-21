package com.laurotc.estacionamentos_tcc2;

import android.util.Log;
import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

/**
 * Created by laurotc on 9/8/16.
 */
public class DetectActivityIntent extends IntentService {

    protected static final String TAG = "DetectedActivityIS";

    public DetectActivityIntent() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Handles incoming intents.
     * @param intent Intent (inside PendingIntent) sent when requestActivityUpdates() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            Intent localIntent = new Intent(Constants.BROADCAST_ACTION);

            // Get the list of the probable activities with its confidence level
            ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

            // Log each activity
            for (DetectedActivity da : detectedActivities) {
                Log.i(TAG, "Activity detected: "
                        + Constants.getActivityString(getApplicationContext(), da.getType()) + " "
                        + da.getConfidence() + "%"
                );
            }

            // Broadcast the list of detected activities
            localIntent.putExtra(Constants.ACTIVITY_EXTRA, detectedActivities);
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        }
    }
}
