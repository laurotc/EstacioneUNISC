package com.laurotc.estacionamentos_tcc2;

import android.os.Bundle;
import android.app.PendingIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.FragmentActivity;
import android.location.Location;
import android.view.View;
import android.util.Log;

import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.DetectedActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        ResultCallback<Status>
{

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location myCurrentLocation;
    private LocationRequest mLocationRequest;
    private ActivityRecognitionRequest mActivitiesRequest;
    private EditText longitudeText, latitudeText, speedText, accelerationText;
    private TextView type;
    private long detectionInterval = 50;
    protected static final String TAG = "MainActivity";

    /**
     * A receiver for DetectedActivity objects broadcast by the
     * {@code ActivityDetectionIntentService}.
     */
    protected ActivityDetectionBroadcastReceiver mBroadcastReceiver;

    /**
     * The DetectedActivities that we track in this sample. We use this for initializing the
     * {@code DetectedActivitiesAdapter}. We also use this for persisting state in
     * {@code onSaveInstanceState()} and restoring it in {@code onCreate()}. This ensures that each
     * activity is displayed with the correct confidence level upon orientation changes.
     */
    private ArrayList<DetectedActivity> mDetectedActivities;

    //private SensorManager mSensorManager;
    //private Sensor mAccelerometer;
    //private double acceleration;
    //private double gravity = 9.8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        longitudeText = (EditText) findViewById(R.id.longitudeText);
        latitudeText = (EditText) findViewById(R.id.latitudeText);
        type = (TextView) findViewById(R.id.type);
        speedText = (EditText) findViewById(R.id.speedText);
        //accelerationText = (EditText) findViewById(R.id.accelerationText);

        //Obtain the SupportMapFragment and get notified when the map is ready to be used.
        //SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        //        .findFragmentById(R.id.map);
        //mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();

        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();

        // Reuse the value of mDetectedActivities from the bundle if possible. This maintains state
        // across device orientation changes. If mDetectedActivities is not stored in the bundle,
        // populate it with DetectedActivity objects whose confidence is set to 0.
        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.DETECTED_ACTIVITIES)) {
            mDetectedActivities = (ArrayList<DetectedActivity>) savedInstanceState.getSerializable(
                    Constants.DETECTED_ACTIVITIES);
        } else {
            mDetectedActivities = new ArrayList<DetectedActivity>();
            // Set the confidence level of each monitored activity to zero.
            for (int i = 0; i < Constants.MONITORED_ACTIVITIES.length; i++) {
                mDetectedActivities.add(new DetectedActivity(Constants.MONITORED_ACTIVITIES[i], 0));
            }
        }
    }


    /**
     * Manipulates the map once available. This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in SCS and move the camera
        LatLng scs = new LatLng(-29.7301234,-52.4306520);
        mMap.addMarker(new MarkerOptions().position(scs).title("Marker in SCS"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(scs));
    }


    /*****
     * App behavior functions
     */
    protected void onStart() {
        /* Connect to Google API Client*/
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        //mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stopLocationUpdates();
        //mSensorManager.unregisterListener(this);
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        //if (mGoogleApiClient.isConnected()) {
        //    startLocationUpdates();
        //}
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION));
    }


    /*****
     * Location functions
     */
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(50);
        mLocationRequest.setFastestInterval(50);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onLocationChanged(Location location) {
        myCurrentLocation = location;
        this.updateUI();
    }


    /*****
     * Update user interface function
     */
    private void updateUI() {
        if (myCurrentLocation!=null) {
            double speedKmh = myCurrentLocation.getSpeed() * (3.6);  // m/s*(3.6) = km/h
            latitudeText.setText(String.valueOf(myCurrentLocation.getLatitude()));
            longitudeText.setText(String.valueOf(myCurrentLocation.getLongitude()));
            speedText.setText(String.valueOf(speedKmh + " km/h"));

            //Person speed avg: 7kmh
            //Running speed avg: 15kmh
            //Biking speed avg: 20kmh (not used for now)
        /*
        if (speedKmh <= 15 && speedKmh > 7) {
            type.setText(String.valueOf("Running"));
        }
        else if (speedKmh <= 7) {
            type.setText(String.valueOf("Walking or stopped"));
        }
        else {
            type.setText(String.valueOf("Probably Car"));
        }
        */
        }
    }


    /*****
     * On connected to Google API functions
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.
                checkLocationSettings(mGoogleApiClient, builder.build());

        this.createLocationRequest();
        this.startLocationUpdates();
        this.startActivitiesUpdates();

        myCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        this.updateUI();
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}


    /******
     * Detect Activities functions
     */
    protected void startActivitiesUpdates() {
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient,
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS, getActivityDetectionPendingIntent());
    }


    /*
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Toggle the status of activity updates requested, and save in shared preferences.
            boolean requestingUpdates = !getUpdatesRequestedState();
            setUpdatesRequestedState(requestingUpdates);
        } else {
            Log.e(TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }

    /**
     * Retrieves a SharedPreference object used to store or read values in this app. If a
     * preferences file passed as the first argument to {@link #getSharedPreferences}
     * does not exist, it is created when {@link SharedPreferences.Editor} is used to commit
     * data.
     */
    private SharedPreferences getSharedPreferencesInstance() {
        return getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    }

    /**
     * Retrieves the boolean from SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    private boolean getUpdatesRequestedState() {
        return getSharedPreferencesInstance()
                .getBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, false);
    }

    /**
     * Sets the boolean in SharedPreferences that tracks whether we are requesting activity updates.
     */
    private void setUpdatesRequestedState(boolean requestingUpdates) {
        getSharedPreferencesInstance()
                .edit()
                .putBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, requestingUpdates)
                .commit();
    }

    /**
     * Stores the list of detected activities in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.e(TAG, "****************Debug - Save instances activities detected****************");
        savedInstanceState.putSerializable(Constants.DETECTED_ACTIVITIES, mDetectedActivities);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectActivityIntent.class);
        this.startService(intent);

        // FLAG_UPDATE_CURRENT is used so that the same pending intent can be used again when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /*
     * Updates the list of activities with the new confidence level
     */
    protected void updateDetectedActivities(ArrayList<DetectedActivity> detectedActivities) {
        HashMap<Integer, Integer> detectedActivitiesMap = new HashMap<>();
        for (DetectedActivity activity : detectedActivities) {
            detectedActivitiesMap.put(activity.getType(), activity.getConfidence());
        }

        // Every time the app detect new activities, it reset all the confidence levels
        ArrayList<DetectedActivity> tempList = new ArrayList<DetectedActivity>();
        for (int i = 0; i < Constants.MONITORED_ACTIVITIES.length; i++) {
            // If a new activity was detected, its confidence level is used.
            // Otherwise, the confidence level used is zero.
            int confidence = 0;
            if (detectedActivitiesMap.containsKey(Constants.MONITORED_ACTIVITIES[i])) {
                confidence = detectedActivitiesMap.get(Constants.MONITORED_ACTIVITIES[i]);
            }
            tempList.add(new DetectedActivity(Constants.MONITORED_ACTIVITIES[i], confidence));
        }

        // Clear Text.
        this.type.setText("");

        // Adding list of activities and its confidence
        String typeList = "";
        for (DetectedActivity detectedActivity: tempList) {
            typeList = (String) this.type.getText();
            this.type.setText(typeList + "\n"
                    + Constants.getActivityString(this, detectedActivity.getType()) + " - "
                    + detectedActivity.getConfidence());

        }
    }

    /**
     * Receiver for intents sent by DetectedActivitiesIntentService via a sendBroadcast().
     * Receives a list of one or more DetectedActivity objects associated with the current state of
     * the device.
     */
    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> updatedActivities =
                    intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);
            updateDetectedActivities(updatedActivities);
        }
    }
}
