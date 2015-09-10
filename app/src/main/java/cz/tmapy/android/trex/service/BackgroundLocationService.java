package cz.tmapy.android.trex.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import cz.tmapy.android.trex.Const;
import cz.tmapy.android.trex.MainScreen;
import cz.tmapy.android.trex.R;
import cz.tmapy.android.trex.database.LocationsDataSource;
import cz.tmapy.android.trex.database.dobs.LocationDob;

/**
 * BackgroundLocationService used for tracking user location in the background.
 * https://gist.github.com/blackcj/20efe2ac885c7297a676
 */
public class BackgroundLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    IBinder mBinder = new LocalBinder();

    private static final String TAG = BackgroundLocationService.class.getName();
    private int NOTIFICATION = 1975; //Unique number for this notification

    private final int MAX_BUFFER_SIZE = 5000; //1 day when send every 20sec

    private PowerManager.WakeLock mPartialWakeLock;

    private GoogleApiClient mGoogleApiClient;

    SharedPreferences mSharedPref;

    KalmanLatLong mKalman;

    private Location mLastAcceptedLocation; //last accepted location, filtered by Mr. Kalman
    private List<Location> locationsToSend = new ArrayList<Location>(); //cach of locations to send

    private String mServerResponse;
    private String mTargetServerURL;
    private String mDeviceIdentifier;
    private String mListPrefs;
    private Integer mLocFrequency;
    private Integer mMinDistance;
    private Integer mSendInterval;
    private Integer mKalmanMPS;

    public class LocalBinder extends Binder {
        public BackgroundLocationService getServerInstance() {
            return BackgroundLocationService.this;
        }
    }

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
        mKalmanMPS = Integer.valueOf(mSharedPref.getString(Const.PREF_KALMAN_MPS, "3"));

        // Kalman filters generally work better when the accuracy decays a bit quicker than one might expect,
        // so for walking around with an Android phone I find that Q=3 metres per second works fine,
        // even though I generally walk slower than that.
        // But if travelling in a fast car a much larger number should obviously be used.
        mKalman = new KalmanLatLong(mKalmanMPS);

        //Keep CPU on
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mPartialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TRexWakelockTag");
        mPartialWakeLock.acquire();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

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
                    acceptLocation(location);
                    return;
                }

                //calculate seconds diff between last and current location
                long diffSeconds = (location.getTime() - mLastAcceptedLocation.getTime()) / 1000;

                if ((diffSeconds >= mSendInterval) || (mLastAcceptedLocation.distanceTo(location) >= mMinDistance)) {
                    acceptLocation(location);
                }

            } catch (Exception e) {
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                if (Const.LOG_BASIC) Log.e(TAG, "Error during location parsing:", e);
            }
        }
    }

    private void acceptLocation(Location location) {
        mLastAcceptedLocation = location;  //location is accepted even if it is not possible to send
        //locationsDataSource.createLocation(new LocationDob(location));
        SendAcceptedLocationBroadcast();
        SendPosition(location);
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
    private void SendPosition(Location location) {

        if (mTargetServerURL != null && !mTargetServerURL.isEmpty()) {
            locationsToSend.add(location);
            if (locationsToSend.size() > MAX_BUFFER_SIZE) locationsToSend.remove(0); //if buffer overfill, remove first in list
            if (isNetworkOnline()) {
                URL url = null;
                try {
                    url = new URL(mTargetServerURL);
                } catch (Exception e) {
                    Toast.makeText(this, "Bad URL: '" + mTargetServerURL + "'", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Bad URL", e);
                    return;
                }

                NetworkTask nt = new NetworkTask(url, mDeviceIdentifier);
                nt.execute(locationsToSend);

                if (locationsToSend.size() > 1)
                    Toast.makeText(this, R.string.buffered_locations_sent, Toast.LENGTH_SHORT).show();

                locationsToSend = new ArrayList<Location>(); //Clean buffer

                if (Const.LOG_ENHANCED)
                    Log.i(TAG, "Position sent to server - lat: " + location.getLatitude() + ", lon: " + location.getLongitude());
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
     * Broadcasts the Intent to receivers in this app.
     */
    private void SendAcceptedLocationBroadcast() {
        Intent localIntent = new Intent(Const.LOCATION_BROADCAST);
        localIntent.putExtra(Const.EXTRAS_POSITION_DATA, mLastAcceptedLocation);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    /**
     * Broadcasts the Intent to receivers in this app.
     */
    private void SendServerResponseBroadcast() {
        Intent localIntent = new Intent(Const.LOCATION_BROADCAST);
        localIntent.putExtra(Const.EXTRAS_SERVER_RESPONSE, mServerResponse);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    @Override
    public void onDestroy() {

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

    /**
     * Private class for sending data
     */
    private class NetworkTask extends AsyncTask<List<Location>, Void, String> {

        private URL mTargetUrl;
        private String mDeviceId;

        public NetworkTask(URL url, String deviceId) {
            mTargetUrl = url;
            mDeviceId = deviceId;
        }

        @Override
        protected String doInBackground(List<Location>... locations) {
            String lastResponse = "";
            try {
                for (Location location : locations[0]) {
                    lastResponse = ""; //reset last response for each location
                    HttpURLConnection conn = (HttpURLConnection) mTargetUrl.openConnection();
                    conn.setReadTimeout((mLocFrequency - 1) * 1000);
                    conn.setConnectTimeout((mLocFrequency - 1) * 1000);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    HashMap<String, String> postDataParams = new HashMap<>();
                    postDataParams.put("i", mDeviceId);
                    postDataParams.put("t", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(location.getTime()));//2014-06-28T15:07:59
                    postDataParams.put("a", Double.toString(location.getLatitude()));
                    postDataParams.put("o", Double.toString(location.getLongitude()));
                    postDataParams.put("l", Double.toString(location.getAltitude()));
                    postDataParams.put("s", Float.toString(location.getSpeed()));
                    postDataParams.put("b", Float.toString(location.getBearing()));

                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(getPostDataString(postDataParams));
                    writer.flush();
                    writer.close();
                    os.close();

                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = br.readLine()) != null) {
                            lastResponse += line;
                        }
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
            mServerResponse = result;
            SendServerResponseBroadcast();
        }
    }
}