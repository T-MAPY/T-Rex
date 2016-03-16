package cz.tmapy.android.trex.service;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.support.v4.content.ContextCompat;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.HttpsURLConnection;

import cz.tmapy.android.trex.Const;
import cz.tmapy.android.trex.MainScreen;
import cz.tmapy.android.trex.R;
import cz.tmapy.android.trex.security.SecurityHelper;

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
    private final int CONNECTION_TIMEOUT = 10000;
    private final int READ_TIMEOUT = 10000;

    IBinder mBinder = new LocalBinder();
    SharedPreferences mSharedPref;
    KalmanLatLong mKalman;
    Geocoder geocoder;
    private int NOTIFICATION = 1975; //Unique number for this notification
    private PowerManager.WakeLock mPartialWakeLock;
    private GoogleApiClient mGoogleApiClient;
    private String mTargetServerURL;
    private String mDeviceIdentifier;
    private String mAccessKey;
    private String mListPrefs;
    private Integer mLocFrequency;
    private Integer mMinDistance;
    private Integer mSendInterval;
    private Integer mKalmanMPS;
    private Boolean mGeocoding;
    private LocationWrapper mFirstLocation; //first accepted location
    private LocationWrapper mLastAcceptedLocation; //last accepted location, filtered by Mr. Kalman

    //thread-safe cache of accepted locations to send
    ConcurrentLinkedQueue<LocationWrapper> mAcceptedLocations = new ConcurrentLinkedQueue<>();

    private Long mStartTime = 0l;
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
        mAccessKey = mSharedPref.getString(Const.PREF_KEY_ACCESS_KEY, null);
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

            mStartTime = System.currentTimeMillis();

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
/*        Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (loc != null) {
            //Kalman initiation
            mKalman.SetState(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy(), loc.getTime());
            processAcceptedLocation(loc);
        }*/

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(mLocFrequency * 1000);
        mLocationRequest.setFastestInterval(1000);

        switch (mListPrefs) {
            case "PRIORITY_BALANCED_POWER_ACCURACY":
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                break;
            case "PRIORITY_LOW_POWER":
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                break;
            case "PRIORITY_NO_POWER":
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                break;
            default:
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                break;
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
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
        evaluateNewLocation(location);
    }

    /**
     * Evaluate new location
     *
     * @param location
     */
    private void evaluateNewLocation(Location location) {
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

        if (Const.LOG_ENHANCED)
            Log.d(TAG, "New position at: " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(loc.getLocation().getTime()) + " (" + loc.getLocation().getLatitude() + ", " + loc.getLocation().getLongitude() + ")");
        //add location to list of accepted
        mAcceptedLocations.add(loc);
        sendPositionsToServer();

        storePositionToPrefs(loc);
        sendAcceptedLocationBroadcast(loc);
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
     */
    private void sendPositionsToServer() {
        if (mTargetServerURL != null && !mTargetServerURL.isEmpty()) {
            URL url = null;
            try {
                url = new URL(mTargetServerURL);
            } catch (Exception e) {
                Toast.makeText(this, "Bad URL: '" + mTargetServerURL + "'", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Bad URL", e);
                return;
            }

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                //if buffer overfill, remove first in queue
                if (mAcceptedLocations.size() > MAX_BUFFER_SIZE) {
                    mAcceptedLocations.remove();
//                    Iterator<LocationWrapper> iterator = mAcceptedLocations.iterator();
//                    iterator.next();
//                    iterator.remove();
                }

                new NetworkTask(url, mDeviceIdentifier).execute(mAcceptedLocations);

            } else {
                Toast.makeText(this, getResources().getString(R.string.perm_internet_for_send_location_not_granted), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.set_target_url, Toast.LENGTH_LONG).show();
            if (Const.LOG_BASIC) Log.e(TAG, "Missing target server URL");
        }
    }

    /**
     * Store position info preferences - it is used by main acitivity when it is resumed
     *
     * @param loc
     */
    private void storePositionToPrefs(LocationWrapper loc) {
        mSharedPref.edit().putLong(Const.START_TIME, mStartTime).apply();
        mSharedPref.edit().putLong(Const.LAST_LOCATION_TIME, loc.getLocation().getTime()).apply();
        mSharedPref.edit().putFloat(Const.ACCURACY, loc.getLocation().getAccuracy()).apply();
        //convert double into long - //http://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
        mSharedPref.edit().putLong(Const.ALTITUDE, Double.doubleToRawLongBits(loc.getLocation().getAltitude())).apply();
        mSharedPref.edit().putFloat(Const.SPEED, loc.getLocation().getSpeed()).apply();
        mSharedPref.edit().putFloat(Const.DISTANCE, mDistance).apply();
    }

    /**
     * Sends broadcasts with location
     */
    private void sendAcceptedLocationBroadcast(LocationWrapper location) {
        Intent localIntent = new Intent(Const.LOCATION_BROADCAST);
        localIntent.putExtra(Const.POSITION, location.getLocation());
        localIntent.putExtra(Const.DISTANCE, mDistance);
        localIntent.putExtra(Const.START_TIME, mStartTime);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    /**
     * Broadcasts the Intent with server response
     */
    private void sendServerResponseBroadcast(String serverResponse) {
        if (mFirstLocation.getServerResponse() == null)
            mFirstLocation.setServerResponse(serverResponse);
        mLastAcceptedLocation.setServerResponse(serverResponse);

        mSharedPref.edit().putString(Const.SERVER_RESPONSE, serverResponse).apply();

        Intent localIntent = new Intent(Const.LOCATION_BROADCAST);
        localIntent.putExtra(Const.SERVER_RESPONSE, serverResponse);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    /**
     * Broadcast the Intent with address
     *
     * @param result
     */
    private void sendAddressBroadcast(String result) {
        if (mFirstLocation.getAddress() == null)
            mFirstLocation.setAddress(result);
        mLastAcceptedLocation.setAddress(result);

        mSharedPref.edit().putString(Const.ADDRESS, result).apply();

        Intent localIntent = new Intent(Const.LOCATION_BROADCAST);
        localIntent.putExtra(Const.ADDRESS, result);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    /**
     * Final broadcast
     */
    private void sendFinalBroadcast() {
        Intent localIntent = new Intent(Const.LOCATION_BROADCAST);

        localIntent.putExtra(Const.START_TIME, mStartTime);
        if (mFirstLocation != null && mFirstLocation.getLocation() != null) {
            localIntent.putExtra(Const.FIRST_LAT, mFirstLocation.getLocation().getLatitude());
            localIntent.putExtra(Const.FIRST_LON, mFirstLocation.getLocation().getLongitude());
            localIntent.putExtra(Const.FIRST_ADDRESS, mFirstLocation.getAddress());
        }
        if (mLastAcceptedLocation != null && mLastAcceptedLocation.getLocation() != null)
        {
            localIntent.putExtra(Const.LAST_LAT, mLastAcceptedLocation.getLocation().getLatitude());
            localIntent.putExtra(Const.LAST_LON, mLastAcceptedLocation.getLocation().getLongitude());
            localIntent.putExtra(Const.LAST_ADDRESS, mLastAcceptedLocation.getAddress());
        }
        localIntent.putExtra(Const.DISTANCE, mDistance);
        localIntent.putExtra(Const.MAX_SPEED, mMaxSpeed);
        localIntent.putExtra(Const.AVE_SPEED, mSpeedLocationsCount > 0 ? mSpeedSum / mSpeedLocationsCount : 0f);
        localIntent.putExtra(Const.MIN_ALT, mMinAltitude);
        localIntent.putExtra(Const.MAX_ALT, mMaxAltitude);
        localIntent.putExtra(Const.ELEV_DIFF_UP, mElevDiffUp);
        localIntent.putExtra(Const.ELEV_DIFF_DOWN, mElevDiffDown);

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    @Override
    public void onDestroy() {

        sendFinalBroadcast();

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
    private class NetworkTask extends AsyncTask<ConcurrentLinkedQueue<LocationWrapper>, Void, String> {

        private URL mTargetUrl;
        private String mDeviceId;

        public NetworkTask(URL url, String deviceId) {
            mTargetUrl = url;
            mDeviceId = deviceId;
        }

        @Override
        protected String doInBackground(ConcurrentLinkedQueue<LocationWrapper>... locations) {
            String lastResponse = "";
            try {
                for (LocationWrapper wrapper : locations[0]) {
                    if (isNetworkOnline()) { //check network for each locations - if offline exist sending cycle
                        lastResponse = ""; //reset last response for each location
                        HttpURLConnection conn = (HttpURLConnection) mTargetUrl.openConnection();
                        conn.setConnectTimeout(CONNECTION_TIMEOUT);
                        conn.setReadTimeout(READ_TIMEOUT);
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
                        postDataParams.put("k", SecurityHelper.GetSecurityString(mDeviceIdentifier, new Date(wrapper.getLocation().getTime()), mAccessKey));

                        if (Const.LOG_ENHANCED)
                            Log.d(TAG, "Sending position: " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(wrapper.getLocation().getTime()));

                        OutputStream os = conn.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                        writer.write(getPostDataString(postDataParams));
                        writer.flush();
                        writer.close();
                        os.close();

                        int responseCode = conn.getResponseCode();

                        //accept all Successful 2xx responses
                        if (responseCode >= HttpsURLConnection.HTTP_OK && responseCode <= HttpsURLConnection.HTTP_PARTIAL) {
                            locations[0].remove();
                            lastResponse = "OK";
                            String serverResponse = "";
                            if (Const.LOG_ENHANCED) {
                                String line;
                                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                while ((line = br.readLine()) != null) {
                                    serverResponse += line;
                                }
                            }
                            if (Const.LOG_ENHANCED)
                                Log.i(TAG, "Server response: " + serverResponse);
                        } else {
                            if (Const.LOG_ENHANCED)
                                Log.w(TAG, "Server response: " + responseCode);
                            lastResponse = getResources().getString(R.string.http_server_response) + ": " + responseCode;
                        }
                    } else {
                        sendServerResponseBroadcast(getResources().getString(R.string.cannot_connect_to_server));
                        if (Const.LOG_ENHANCED)
                            Log.w(TAG, "Cannot connect to server: '" + mTargetServerURL + "'");
                        lastResponse = getResources().getString(R.string.http_server_not_accessible);
                        break; //exit sending loop - try it again with next position
                    }
                }
            } catch (java.net.SocketTimeoutException e) {
                Log.e(TAG, "Connection timeout!", e);
                lastResponse = getResources().getString(R.string.http_server_not_accessible);
            } catch (java.io.IOException e) {
                Log.e(TAG, "Cannot read server response", e);
                lastResponse = getResources().getString(R.string.http_server_not_accessible);
            } catch (Exception e) {
                Log.e(TAG, "HTTP connection error", e);
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
            sendServerResponseBroadcast(result);
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
                sendAddressBroadcast(result);
        }
    }

}