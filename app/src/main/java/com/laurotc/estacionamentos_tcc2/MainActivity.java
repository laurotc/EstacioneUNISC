package com.laurotc.estacionamentos_tcc2;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.PendingIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.FragmentActivity;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import android.view.View;
import android.widget.Button;
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
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.DetectedActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static android.R.id.list;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        ResultCallback<Status> {

    protected static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;

    private Location myCurrentLocation;
    private LocationRequest mLocationRequest;
    private GoogleMap mMap;
    private Marker marker;
    private LatLngBounds uniscBounds;
    private LatLng uniscNE = new LatLng(Constants.NE_BOUND_LAT, Constants.NE_BOUND_LONG);
    private LatLng uniscSW = new LatLng(Constants.SW_BOUND_LAT, Constants.SW_BOUND_LONG);

    private TextView type, longitudeText, latitudeText, speedText;
    private Button updateMapButton, clusterMapButton, heatmapMapButton, markerMapButton;
    private boolean viewClusters, viewHeatmaps, viewMarkers;
    private ClusterManager<ClusterItemMap> mClusterManager;

    final private DatabaseHandler db = new DatabaseHandler(this);

    //A receiver for DetectedActivity objects broadcast
    protected ActivityDetectionBroadcastReceiver mBroadcastReceiver;

    //The DetectedActivities that is tracked
    private PendingIntent pendingIntent;
    private ArrayList<DetectedActivity> mDetectedActivities;
    private ActivityRecognitionRequest mActivitiesRequest;
    private boolean detectActivityRunning = false;
    private TileOverlay mOverlay = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(0xFFFFFFFF); //White
        setSupportActionBar(toolbar);

        longitudeText = (TextView) findViewById(R.id.longitudeText);
        latitudeText = (TextView) findViewById(R.id.latitudeText);
        speedText = (TextView) findViewById(R.id.speedText);
        updateMapButton = (Button) findViewById(R.id.update);


        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.BROADCAST_ACTION));

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

        connectGoogleServices();
        initGoogleMap();
        uniscBounds = new LatLngBounds(uniscSW, uniscNE);
    }

    /*****
     * App behavior functions
     */
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        //mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        //stopLocationUpdates();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        //if (mGoogleApiClient.isConnected()) { startLocationUpdates(); }
    }

    /*****
     * Connect to Google API Services
     */
    protected synchronized void connectGoogleServices() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        //LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        //PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        this.createLocationRequest();
        this.startLocationUpdates();
        if (!detectActivityRunning) {
            this.startActivitiesUpdates();
        }
        updateLocationInfo();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ERROR" + connectionResult.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }


    /*****
     * Manipulates the map when it's available. This callback is triggered when the map is ready to be used.
     */
    protected void initGoogleMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap = googleMap;
            mMap.setMaxZoomPreference(19);
            mMap.setMinZoomPreference(15);
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);

            DeviceInfo deviceInfo = db.getTableData();
            viewClusters = true;
            viewHeatmaps = false;
            viewMarkers = false;

            if (deviceInfo.getStatus() == Constants.PARKED || deviceInfo.getStatus() == Constants.NOT_PARKED) {
                Log.i(TAG, "new data -> Lat: " + deviceInfo.getLatitude() + " | Long: "
                        + deviceInfo.getLongitude() + " | Parked: " + deviceInfo.getStatus());

                LatLng latlng = new LatLng(deviceInfo.getLatitude(), deviceInfo.getLongitude());
                addMarkerMap(latlng);

                updateMapData(db.getTableData());
            }

            /*Button actions*/
            updateMapButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    updateMapData(db.getTableData());
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            //sendEmailException(e.getMessage());
        }
    }

    public void addMarkerMap(LatLng latlng) {
        this.marker = mMap.addMarker(new MarkerOptions().position(latlng).title("Você estacionou aqui"));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latlng, 17);
        mMap.animateCamera(cameraUpdate);
    }

    public void removeMarkerMap(Marker marker) {
        marker.remove();
    }


    /*****
     * Location functions
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(50);
        mLocationRequest.setFastestInterval(50);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//PRIORITY_BALANCED_POWER_ACCURACY
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        myCurrentLocation = location;
        updateLocationInfo();
    }

    public void onStartService(View v) {
        Intent i = new Intent(this, DetectActivityIntent.class);
        startService(i);
    }

    /******
     * Detect Activities functions
     */

    /*
     * Start activities update
     */
    protected void startActivitiesUpdates() {
        Intent intent = new Intent(this, DetectActivityIntent.class);
        this.startService(intent);

        pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, Constants.DETECTION_INTERVAL_IN_MILLISECONDS, pendingIntent);

        detectActivityRunning = true;
    }

    /*
     * Called when the result of calling requestActivityUpdates() and removeActivityUpdates() are available.
     * @param status Status returned through a PendingIntent when requestActivityUpdates() or removeActivityUpdates() are called.
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
     * Receiver for intents sent by DetectedActivitiesIntentService via a sendBroadcast().
     * Receives a list of one or more DetectedActivity objects associated with the current state of the device.
     */
    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> updatedActivities = intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);

            try {
                updateDetectedActivities(updatedActivities);
            } catch (Exception e) {
                Log.e(TAG, "Error detecting or sending data do server: " + e.getMessage());
                //sendEmailException("Error detecting or sending data do server: " + e.getMessage());
            }
        }
    }

    /*****
     * Update user interface function
     */
    private void updateLocationInfo() {
        if (myCurrentLocation!=null) {
            latitudeText.setText(String.valueOf("Lat: " + myCurrentLocation.getLatitude()));
            longitudeText.setText(String.valueOf("Long: " + myCurrentLocation.getLongitude()));

            double speedKmh = Constants.speedInKmh(myCurrentLocation.getSpeed());
            speedText.setText(String.valueOf(speedKmh + " km/h "));
        }
    }

    private void updateMapData(DeviceInfo deviceInfo) {
        try {
            String jsonDevice = Utils.makeJsonDevice(deviceInfo, Constants.READ, this);
            new CallServerHandler().execute(jsonDevice);
        }
        catch (Exception e) {
            Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Updates the list of activities with the new confidence level
     */
    public void updateDetectedActivities(ArrayList<DetectedActivity> detectedActivities) throws UnsupportedEncodingException, JSONException {
        //If Google API is not connected and current location is off, it connects again before update info
        if (mGoogleApiClient == null && myCurrentLocation == null) {
            connectGoogleServices();
        }

        //Add all detected activities to a Map (type, confidence)
        HashMap<Integer, Integer> detectedActivitiesMap = new HashMap<>();
        for (DetectedActivity activity : detectedActivities) {
            detectedActivitiesMap.put(activity.getType(), activity.getConfidence());
        }

        // Every time the app detect new activities, it reset all the confidence levels
        ArrayList<DetectedActivity> tempList = new ArrayList<>();
        for (int i = 0; i < Constants.MONITORED_ACTIVITIES.length; i++) {
            // If a new activity was detected, its confidence level is used, otherwise, the confidence level used is zero.
            int confidence = 0;
            if (detectedActivitiesMap.containsKey(Constants.MONITORED_ACTIVITIES[i])) {
                confidence = detectedActivitiesMap.get(Constants.MONITORED_ACTIVITIES[i]);
            }
            tempList.add(new DetectedActivity(Constants.MONITORED_ACTIVITIES[i], confidence));
        }

        //Data saved in the Database until this moment (Old position)
        DeviceInfo deviceInfo = db.getTableData();

        //New data, with position updated, if status is different from the one saved already then updates de database, otherwise keeps the old data
        DeviceInfo deviceInfoUpdated = new DeviceInfo();
        deviceInfoUpdated.setLatitude(myCurrentLocation.getLatitude());
        deviceInfoUpdated.setLongitude(myCurrentLocation.getLongitude());
        deviceInfoUpdated.setStatus(-1); // Set -1 to say the database is not gonna be changed

        //Foreach activity detected
        for (DetectedActivity detectedActivity: tempList) {
            //If is Parked
            if (deviceInfo.getStatus() == Constants.PARKED) {
                //Log.i(TAG, "Parked");
                if (detectedActivity.getType() == DetectedActivity.IN_VEHICLE && detectedActivity.getConfidence() >= 70
                    && Constants.speedInKmh(myCurrentLocation.getSpeed()) >= 15) {
                    //Then it checks the confidence and speed, if it matches saves in the database
                    //If so, saves in the database as not parked anymore, person is now driving
                    //Vehicle speed avg >= 15kmh
                    //Log.d(TAG, "In Vehicle | Confidence >= 70% | Speed >= 15kmh");
                    deviceInfoUpdated.setStatus(Constants.NOT_PARKED);
                }
            } else {
                //Log.i(TAG, "Not Parked");
                if ((detectedActivity.getType() == DetectedActivity.ON_FOOT || detectedActivity.getType() == DetectedActivity.TILTING)
                        && detectedActivity.getConfidence() >= 70 && Constants.speedInKmh(myCurrentLocation.getSpeed()) <= 10) {
                    //Then it checks if the new activity detected is ON FOOT or TILTING, confidence >= 70 and speed <= 10
                    //And saves it as new current activity, which will say the vehicle is PARKED with the current position
                    //Person speed avg: <= 10kmh
                    //Log.d(TAG, "On Foot or Tilting | Confidence >= 50% | Speed <= 10kmh");
                    deviceInfoUpdated.setStatus(Constants.PARKED);
                } else if (detectedActivity.getType() == DetectedActivity.STILL && detectedActivity.getConfidence() >= 70
                        && myCurrentLocation.getSpeed() <= 2) { // speed less than 2m/s
                    //If the new activity is STILL and confidence >= 70, then the device is resting somewhere and speed is less than 2m/s
                    //the app is gonna save this state as PARKED with the current position
                    //Log.d(TAG, "Still | Confidence >= 70% | Speed is around 2m/s");
                    deviceInfoUpdated.setStatus(Constants.PARKED);
                }
            }
        }

        //Update Database only if the status is different from the one already saved and is one the cases above
        if (deviceInfoUpdated.getStatus() >= 0 && deviceInfoUpdated.getStatus() != deviceInfo.getStatus()) {
            db.updateTableData(deviceInfoUpdated);
            LatLng latlng = new LatLng(deviceInfoUpdated.getLatitude(), deviceInfoUpdated.getLongitude());
            Toast.makeText(this, Constants.getActivityStringToast(this, deviceInfoUpdated.getStatus()), Toast.LENGTH_LONG).show();

            //It only parks if the location is inside UNISC bounds
            if (uniscBounds.contains(latlng) && deviceInfoUpdated.getStatus() == Constants.PARKED) {
                //Send to server info PARKED, with position saved into the database
                //Log.i(TAG, "Ponto está na UNISC");
                addMarkerMap(latlng);
            }

            //To remove the marker and set as NOT_PARKED it can be from anywhere
            //If is leaving the parking lot, then it updates the old position with the flag NOT_PARKED and remove marker
            if (deviceInfoUpdated.getStatus() == Constants.NOT_PARKED) {
                //Send to server info NOT_PARKED, and remove from server database
                removeMarkerMap(this.marker);
            }

            try {
                //Send to server info of the new device info
                String jsonDevice = Utils.makeJsonDevice(deviceInfoUpdated, Constants.UPDATE, this);
                new CallServerHandler().execute(jsonDevice);
            }
            catch (Exception e) {
                Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**************************
     * Class to make asynchronous call to the server to save or get the data
     **************************/
    private class CallServerHandler extends AsyncTask<String, Void, String> {

        ServerHandler serverHandler = new ServerHandler();
        protected String doInBackground(String... deviceDataString) {
            try {
                return serverHandler.sendPostData(deviceDataString[0]);
            }
            catch (Exception e) {
                Log.e(TAG, getString(R.string.no_connection));
                //e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String jsonEstacionamentosData) {
            try {
                showStatusEstacionamentos(jsonEstacionamentosData);
            } catch (JSONException e) {
                Log.e(TAG, getString(R.string.no_connection));
                //e.printStackTrace();
            }
        }
    }


    private void showStatusEstacionamentos(String jsonEstacionamentosData) throws JSONException {
        try {
            mMap.clear();
            Log.e(TAG, jsonEstacionamentosData);
            JSONArray array = new JSONArray(jsonEstacionamentosData);
            ArrayList<WeightedLatLng> latLngServerDataWeighted = new ArrayList<>();
            Collection<ClusterItemMap> latLngServerDataCluster = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject row = array.getJSONObject(i);
                LatLng latlng = new LatLng(row.getDouble("latitude"), row.getDouble("longitude"));
                if (row.getInt("status")!=Constants.NOT_PARKED) {
                    WeightedLatLng weightedLatLng = new WeightedLatLng(latlng, 1000);
                    latLngServerDataWeighted.add(weightedLatLng);
                    latLngServerDataCluster.add(new ClusterItemMap(latlng));
                }
            }

            setUpClusters(latLngServerDataCluster);
        }
        catch (Exception e) {
            Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_LONG).show();
        }
    }

    /********************
     * MARKERS
     ********************/
    public void setUpMarkers(LatLng latlng) {
        mMap.addMarker(new MarkerOptions().position(latlng));
    }

    /********************
     * HEAT MAPS
     ********************/
    public void setUpHeatMap(ArrayList<WeightedLatLng> latLngArray)
    {
        mMap.clear();
        addMarkerMap(marker.getPosition());
        // Create gradient, Green & Red
        int[] colors = { Color.rgb(102, 225, 0), Color.rgb(255, 0, 0) };
        float[] startPoints = { 0.2f, 0.4f };

        if (this.mOverlay!=null) { this.mOverlay.remove(); }

        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder().weightedData(latLngArray).build();
        mProvider.setGradient(new Gradient(colors, startPoints));
        mProvider.setRadius(20);
        mProvider.setOpacity(1);

        this.mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }


    /********************
     * CLUSTERING POINTS
     ********************/
    public class ClusterItemMap implements ClusterItem
    {
        private final LatLng mPosition;

        public ClusterItemMap(LatLng latLng) {
            mPosition = latLng;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }
    }

    private void setUpClusters(Collection<ClusterItemMap> latLngClusterData)
    {
        if (mClusterManager != null) {
            mClusterManager.clearItems();
        }

        mClusterManager = new ClusterManager<ClusterItemMap>(this, mMap);

        // Adding cluster markers to the cluster manager for each point.
        mClusterManager.addItems(latLngClusterData);

        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);
    }


    /************************
     * ATTENTION: This was auto-generated
     * **********************
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
        return getSharedPreferencesInstance().getBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, false);
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
        Log.i(TAG, "****************Debug - Save instances activities detected****************");
        savedInstanceState.putSerializable(Constants.DETECTED_ACTIVITIES, mDetectedActivities);
        super.onSaveInstanceState(savedInstanceState);
    }

    public void sendEmailException(String exception) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + "laurotc@mx2.unisc.br"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Exception - Estacione UNISC");
        emailIntent.putExtra(Intent.EXTRA_TEXT, exception);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }
}
