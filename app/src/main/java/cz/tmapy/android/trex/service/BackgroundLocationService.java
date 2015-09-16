package cz.tmapy.android.trex.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import cz.tmapy.android.trex.Const;
import cz.tmapy.android.trex.MainScreen;
import cz.tmapy.android.trex.R;

/**
 * BackgroundLocationService used for tracking user location in the background.
 * https://gist.github.com/blackcj/20efe2ac885c7297a676
 */
public class BackgroundLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = BackgroundLocationService.class.getName();
    private final int MAX_BUFFER_SIZE = 5000; //1 day when send every 20sec
    IBinder mBinder = new LocalBinder();
    SharedPreferences mSharedPref;
    KalmanLatLong mKalman;
    Geocoder geocoder;
    private int NOTIFICATION = 1975; //Unique number for this notification
    private PowerManager.WakeLock mPartialWakeLock;
    private GoogleApiClient mGoogleApiClient;
    private String mTargetServerURL;
    private String mDeviceIdentifier;
    private String mListPrefs;
    private Integer mLocFrequency;
    private Integer mMinDistance;
    private Integer mSendInterval;
    private Integer mKalmanMPS;
    private Boolean mGeocoding;
    private Long mLocationStart; //start of location
    private LocationWrapper mFirstLocation; //first accepted location
    private LocationWrapper mLastAcceptedLocation; //last accepted location, filtered by Mr. Kalman
    private List<LocationWrapper> locationsToSend = new ArrayList<LocationWrapper>(); //cach of locations to send

    private Long mDuration = 0l;
    private Float mDistance = 0f;
    private Float mMaxSpeed = 0f;
    private Float mSpeedSum = 0f;
    private Integer mSpeedLocationsCount = 0; //count of locations with speed
    private Double mMinAltitude = 0d;
    private Double mMaxAltitude = 0d;
    private Double mLastElevationValue = 0d; //last elevation value
    private Double mElevDiffUp = 0d;
    private Double mElevDiffDown = 0d;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mTargetServerURL = mSharedPref.getString(Const.PREF_KEY_TARGET_SERVUER_URL, null);
        mDeviceIdentifier = mSharedPref.getString(Const.PREF_KEY_DEVICE_ID, null);
        mListPrefs = mSharedPref.getString(Const.PREF_LOC_STRATEGY, "PRIORITY_HIGH_ACCURACY");
        mLocFrequency = Integer.valueOf(mSharedPref.getString(Const.PREF_LOC_FREQUENCY, "20"));
        mMinDistance = Integer.valueOf(mSharedPref.getString(Const.PREF_MIN_DIST, "60"));
        mSendInterval = Integer.valueOf(mSharedPref.getString(Const.PREF_SEND_INTERVAL, "120"));
        mKalmanMPS = Integer.valueOf(mSharedPref.getString(Const.PREF_KALMAN_MPS, "15"));
        mGeocoding = mSharedPref.getBoolean(Const.PREF_GEOCODING, false);

        // Kalman filters generally work better when the accuracy decays a bit quicker than one might expect,
        // so for walking around with an Android phone I find that Q=3 metres per second works fine,
        // even though I generally walk slower than that.
        // But if travelling in a fast car a much larger number should obviously be used.
        mKalman = new KalmanLatLong(mKalmanMPS);

        geocoder = new Geocoder(this, Locale.getDefault());

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        //Keep CPU on
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mPartialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TRexWakelockTag");
        mPartialWakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.trainers)
                            .setContentTitle(getResources().getString(R.string.notif_title))
                            .setContentText(getResources().getString(R.string.notif_text))
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(false)
                            .setOngoing(true)
                            .setPriority(Notification.PRIORITY_HIGH);

            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(this, MainScreen.class);
            //resultIntent.putExtra(Const.STATE_LOCALIZATION_IS_RUNNING, true);
            // The stack builder object will contain an artificial back stack for the started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainScreen.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);

            Notification notification = mBuilder.build();
            notification.flags |= Notification.FLAG_NO_CLEAR;

            startForeground(NOTIFICATION, notification); //spuštění služby s vyšší prioritou na popředí - http://developer.android.com/reference/android/app/Service.html

            mLocationStart = System.currentTimeMillis();

            if (Const.LOG_ENHANCED) Log.i(TAG, "Localization Started");
            Toast.makeText(this, R.string.localiz_started, Toast.LENGTH_SHORT).show();
        }

        return START_STICKY;
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (loc != null) {
            //Kalman initiation
            mKalman.SetState(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy(), loc.getTime());
            processLocation(loc);
        }

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(mLocFrequency * 1000);
        mLocationRequest.setFastestInterval(1000);

        switch (mListPrefs) {
            case "PRIORITY_BALANCED_POWER_ACCURACY":
                mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                break;
            case "PRIORITY_LOW_POWER":
                mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
                break;
            case "PRIORITY_NO_POWER":
                mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
                break;
            default:
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                break;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Location Services suspended: " + i, Toast.LENGTH_LONG).show();
        if (Const.LOG_BASIC) Log.e(TAG, "Location Services suspended: " + i);
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Location services failed", Toast.LENGTH_LONG).show();
        if (Const.LOG_BASIC)
            Log.e(TAG, "Connection to location services failed: " + connectionResult.getErrorCode());
    }

    /**
     * Called when the location has changed.
     * <p/>
     * <p> There are no restrictions on the use of the supplied Location object.
     *
     * @param location The new location, as a Location object.
     */
    @Override
    public void onLocationChanged(Location location) {
        processLocation(location);
    }

    /**
     * Process new location
     *
     * @param location
     */
    private void processLocation(Location location) {
        if (location != null) {
            try {
                //If Kalman MPS settings > 0 apply Kalman filter
                if (mKalmanMPS > 0) {
                    mKalman.Process(location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getTime());
                    //update position by Mr. Kalman
                    location.setLatitude(mKalman.get_lat());
                    location.setLongitude(mKalman.get_lng());
                }

                if (mLastAcceptedLocation == null) { //ths is first position
                    mMaxSpeed = location.getSpeed();
                    mSpeedSum = location.getSpeed();
                    mMaxAltitude = location.getAltitude();
                    mMinAltitude = location.getAltitude();
                    acceptLocation(location);
                    return;
                }

                //calculate seconds diff between last and current location
                long diffSeconds = (location.getTime() - mLastAcceptedLocation.getLocation().getTime()) / 1000;
                //calculate distance
                float dist = mLastAcceptedLocation.getLocation().distanceTo(location);

                if ((diffSeconds >= mSendInterval) || dist >= mMinDistance) {
                    mDistance += dist;
                    mDuration = location.getTime() - mLocationStart;

                    if (location.hasSpeed()) //rise number of positions with speed only when it is > 0
                    {
                        mSpeedLocationsCount++;
                        mSpeedSum += location.getSpeed();
                        if (location.getSpeed() > mMaxSpeed) mMaxSpeed = location.getSpeed();
                    }

                    if (location.hasAltitude()) {
                        if (location.getAltitude() > mMaxAltitude)
                            mMaxAltitude = location.getAltitude();
                        if (location.getAltitude() < mMinAltitude || mMinAltitude == 0)
                            mMinAltitude = location.getAltitude();

                        if (mLastElevationValue > 0) {
                            Double d = location.getAltitude() - mLastElevationValue;
                            if (d > 0)
                                mElevDiffUp += d;
                            else
                                mElevDiffDown += Math.abs(d);
                        }
                        mLastElevationValue = location.getAltitude();
                    }

                    acceptLocation(location);
                }

            } catch (Exception e) {
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                if (Const.LOG_BASIC) Log.e(TAG, "Error during location parsing:", e);
            }
        }
    }

    /**
     * Process accepted location
     *
     * @param location
     */
    private void acceptLocation(Location location) {

        LocationWrapper loc = new LocationWrapper();
        loc.setLocation(location);

        if (mFirstLocation == null)
            mFirstLocation = loc;

        mLastAcceptedLocation = loc;  //location is accepted even if it is not possible to send

        if (mGeocoding && Geocoder.isPresent()) {
            new GeocodingTask().execute(location);
        }

        SendAcceptedLocationBroadcast(loc);
        SendPosition(loc);
    }

    /**
     * Checks network connectivity
     *
     * @return
     */
    public boolean isNetworkOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Send location to the server
     *
     * @param location
     */
    private void SendPosition(LocationWrapper location) {

        if (mTargetServerURL != null && !mTargetServerURL.isEmpty()) {
            locationsToSend.add(location);
            if (locationsToSend.size() > MAX_BUFFER_SIZE)
                locationsToSend.remove(0); //if buffer overfill, remove first in list
            if (isNetworkOnline()) {
                URL url = null;
                try {
                    url = new URL(mTargetServerURL);
                } catch (Exception e) {
                    Toast.makeText(this, "Bad URL: '" + mTargetServerURL + "'", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Bad URL", e);
                    return;
                }

                new NetworkTask(url, mDeviceIdentifier).execute(locationsToSend);

                if (locationsToSend.size() > 1)
                    Toast.makeText(this, R.string.buffered_locations_sent, Toast.LENGTH_SHORT).show();

                locationsToSend = new ArrayList<LocationWrapper>(); //Clean buffer

                if (Const.LOG_ENHANCED)
                    Log.i(TAG, "Position sent to server - lat: " + location.getLocation().getLatitude() + ", lon: " + location.getLocation().getLongitude());
            } else {
                Toast.makeText(this, R.string.cannot_connect_to_server, Toast.LENGTH_SHORT).show();
                if (Const.LOG_BASIC)
                    Log.w(TAG, "Cannot connect to server: '" + mTargetServerURL + "'");
            }

        } else {
            Toast.makeText(this, "Missing target server URL", Toast.LENGTH_LONG).show();
            if (Const.LOG_BASIC) Log.e(TAG, "Missing target server URL");
        }
    }

    /**
     * Broadcasts the Intent with location
     */
    private void SendAcceptedLocationBroadcast(LocationWrapper location) {
        Intent localIntent = new Intent(Const.LOCATION_BROADCAST);
        localIntent.putExtra(Const.POSITION, location.getLocation());
        localIntent.putExtra(Const.DISTANCE, mDistance);
        localIntent.putExtra(Const.DURATION, mDuration);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    /**
     * Broadcasts the Intent with server response
     */
    private void SendServerResponseBroadcast(String serverResponse) {
        Intent localIntent = new Intent(Const.LOCATION_BROADCAST);
        localIntent.putExtra(Const.SERVER_RESPONSE, serverResponse);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    /**
     * Broadcast the Intent with address
     *
     * @param result
     */
    private void SendAddressBroadcast(String result) {
        Intent localIntent = new Intent(Const.LOCATION_BROADCAST);
        localIntent.putExtra(Const.ADDRESS, result);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    /**
     * Final broadcast
     */
    private void SendFinalBroadcast() {
        Intent localIntent = new Intent(Const.LOCATION_BROADCAST);

        localIntent.putExtra(Const.START_TIME, mFirstLocation.getLocation().getTime());
        localIntent.putExtra(Const.START_LAT, mFirstLocation.getLocation().getLatitude());
        localIntent.putExtra(Const.START_LON, mFirstLocation.getLocation().getLongitude());
        localIntent.putExtra(Const.START_ADDRESS, mFirstLocation.getAddress());
        localIntent.putExtra(Const.FINISH_TIME, mLastAcceptedLocation.getLocation().getTime());
        localIntent.putExtra(Const.FINISH_LAT, mLastAcceptedLocation.getLocation().getLatitude());
        localIntent.putExtra(Const.FINISH_LON, mLastAcceptedLocation.getLocation().getLongitude());
        localIntent.putExtra(Const.FINISH_ADDRESS, mLastAcceptedLocation.getAddress());
        localIntent.putExtra(Const.DISTANCE, mDistance);
        localIntent.putExtra(Const.MAX_SPEED, mMaxSpeed);
        localIntent.putExtra(Const.AVE_SPEED, mSpeedSum / mSpeedLocationsCount);
        localIntent.putExtra(Const.MIN_ALT, mMinAltitude);
        localIntent.putExtra(Const.MAX_ALT, mMaxAltitude);
        localIntent.putExtra(Const.ELEV_DIFF_UP, mElevDiffUp);
        localIntent.putExtra(Const.ELEV_DIFF_DOWN, mElevDiffDown);

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    @Override
    public void onDestroy() {

        SendFinalBroadcast();

        stopForeground(true); //http://developer.android.com/reference/android/app/Service.html

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

            mGoogleApiClient.disconnect();
            // Destroy the current location client
            mGoogleApiClient = null;
        }

        //remove wakelock
        if (mPartialWakeLock != null && mPartialWakeLock.isHeld()) {
            mPartialWakeLock.release();
        }

        // Display the connection status
        Toast.makeText(this, R.string.localiz_stopped, Toast.LENGTH_SHORT).show();
        if (Const.LOG_ENHANCED) Log.i(TAG, "Localization Stopped");

        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public BackgroundLocationService getServerInstance() {
            return BackgroundLocationService.this;
        }
    }

    /**
     * Private class for sending data
     */
    private class NetworkTask extends AsyncTask<List<LocationWrapper>, Void, String> {

        private URL mTargetUrl;
        private String mDeviceId;

        public NetworkTask(URL url, String deviceId) {
            mTargetUrl = url;
            mDeviceId = deviceId;
        }

        @Override
        protected String doInBackground(List<LocationWrapper>... locations) {
            String lastResponse = "";
            try {
                for (LocationWrapper wrapper : locations[0]) {
                    lastResponse = ""; //reset last response for each location
                    HttpURLConnection conn = (HttpURLConnection) mTargetUrl.openConnection();
                    conn.setReadTimeout((mLocFrequency - 1) * 1000);
                    conn.setConnectTimeout((mLocFrequency - 1) * 1000);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    HashMap<String, String> postDataParams = new HashMap<>();
                    postDataParams.put("i", mDeviceId);
                    postDataParams.put("t", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(wrapper.getLocation().getTime()));//2014-06-28T15:07:59
                    postDataParams.put("a", Double.toString(wrapper.getLocation().getLatitude()));
                    postDataParams.put("o", Double.toString(wrapper.getLocation().getLongitude()));
                    postDataParams.put("l", Double.toString(wrapper.getLocation().getAltitude()));
                    postDataParams.put("s", Float.toString(wrapper.getLocation().getSpeed()));
                    postDataParams.put("b", Float.toString(wrapper.getLocation().getBearing()));

                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(getPostDataString(postDataParams));
                    writer.flush();
                    writer.close();
                    os.close();

                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        if (Const.LOG_ENHANCED) {
                            String line;
                            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            while ((line = br.readLine()) != null) {
                                lastResponse += line;
                            }
                        } else
                            lastResponse = "OK";
                    } else {
                        lastResponse = "HTTP response: " + responseCode;
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Network connection error:", e);
                lastResponse = e.getLocalizedMessage();
            }

            return lastResponse;
        }

        private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            return result.toString();
        }

        /**
         * Response processing
         *
         * @param result
         */
        @Override
        protected void onPostExecute(String result) {
            SendServerResponseBroadcast(result);
        }
    }

    /**
     * Private class for geocoding
     */
    private class GeocodingTask extends AsyncTask<Location, Void, String> {

        @Override
        protected String doInBackground(Location... locations) {
            String response = "";
            List<Address> addresses = null;
            try {
                //Throughfare can be null, FeatureName can be same as SubAdminArea
                addresses = geocoder.getFromLocation(locations[0].getLatitude(), locations[0].getLongitude(), 1);
                if (addresses.size() > 0) {
                    Address address = addresses.get(0);
                    if (address.getThoroughfare() != null)
                        response = response + address.getThoroughfare() + " ";
                    if (address.getFeatureName() != null && !address.getFeatureName().equals(address.getThoroughfare()) && !address.getFeatureName().equals(address.getLocality()) && !address.getFeatureName().equals(address.getSubAdminArea()))
                        response = response + address.getFeatureName() + ", ";
                    if (address.getLocality() != null)
                        response = response + address.getLocality() + ", ";
                    if (address.getSubAdminArea() != null)
                        response = response + address.getSubAdminArea();
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoding exception", e);
            }
            return response;
        }

        /**
         * Response processing
         *
         * @param result
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null && result.length() > 0)
                SendAddressBroadcast(result);
        }
    }

}