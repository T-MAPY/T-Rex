package cz.tmapy.android.trex;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.SimpleDateFormat;

import cz.tmapy.android.trex.update.Updater;

public class MainScreen extends ActionBarActivity {

    private static final String TAG = "MainScreen";

    private String mTargetServerURL;
    private String mDeviceId;
    private Boolean mKeepScreenOn = false;

    private PowerManager.WakeLock mWakeLock;

    //members for state saving
    private String mLastLocationTime;
    private String mLastLocationLat;
    private String mLastLocationLon;
    private String mLastLocationAlt;
    private String mLastLocationSpeed;
    private String mLastLocationBearing;
    private String mLastServerResponse;

    private final String STATE_LAST_LOCATION_TIME = "lastLocationTime";
    private final String STATE_LAST_LOCATION_LAT = "lastLocationLast";
    private final String STATE_LAST_LOCATION_LON = "lastLocationLon";
    private final String STATE_LAST_LOCATION_ALT = "lastLocationAlt";
    private final String STATE_LAST_LOCATION_SPEED = "lastLocationSpeed";
    private final String STATE_LAST_LOCATION_BEARING = "lastLocationBearing";
    private final String STATE_LAST_SERVER_RESPONSE = "lastServerResponse";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggle_start);

        //sets toggle state before listener is attached
        toggle.setChecked(getIntent().getBooleanExtra(Constants.EXTRAS_LOCALIZATION_IS_RUNNING, false));

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!startSending()) //cancel toggle switch when service' start is not successful
                    {
                        toggle.setChecked(false);
                    }
                } else {
                    stopSending();
                }
            }
        });

        //Registrace broadcastreceiveru komunikaci se sluzbou (musi byt tady, aby fungoval i po nove inicializaci aplikace z notifikace
        // The filter's action is BROADCAST_ACTION
        IntentFilter mIntentFilter = new IntentFilter(Constants.LOCATION_BROADCAST);
        // Instantiates a new mPositionReceiver
        NewPositionReceiver mPositionReceiver = new NewPositionReceiver();
        // Registers the mPositionReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(mPositionReceiver, mIntentFilter);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mDeviceId = sharedPref.getString("pref_id", "");
        mTargetServerURL = sharedPref.getString("pref_targetUrl", "");
        mKeepScreenOn = sharedPref.getBoolean("pref_screen_on", false);

        //if this is not orientation change (saved bundle doesn't exists) check for update
        if (savedInstanceState == null && sharedPref.getBoolean("pref_check4update", true))
            new Updater(this).execute();

        //ACRA.getErrorReporter().putCustomData("myKey", "myValue");
        //ACRA.getErrorReporter().handleException(new Exception("Test exception"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Start localizing and sending
     */
    public Boolean startSending() {

        if (!mTargetServerURL.isEmpty()) {
            if (!mDeviceId.isEmpty()) {
                if (!isServiceRunning(BackgroundLocationService.class)) {
                    //Keep CPU on
                    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                    mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            "TRexWakelockTag");
                    mWakeLock.acquire();

                    if (mKeepScreenOn)
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    //Nastartovani sluzby
                    ComponentName comp = new ComponentName(getApplicationContext().getPackageName(), BackgroundLocationService.class.getName());
                    ComponentName service = getApplicationContext().startService(new Intent().setComponent(comp));

                    if (null == service) {
                        // something really wrong here
                        Toast.makeText(this, R.string.localiz_could_not_start, Toast.LENGTH_SHORT).show();
                        if (Constants.LOG_BASIC)
                            Log.e(TAG, "Could not start localization service " + comp.toString());
                        return false;
                    } else
                        return true;
                } else {
                    Toast.makeText(this, R.string.localiz_run, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Set device identifier", Toast.LENGTH_SHORT).show();
                if (Constants.LOG_BASIC) Log.e(TAG, "Device identifier is not setted");
            }
        } else {
            Toast.makeText(this, "Set target server URL", Toast.LENGTH_SHORT).show();
            if (Constants.LOG_BASIC) Log.e(TAG, "Target server URL is not setted");
        }

        return false;
    }

    /**
     * Switch off sending
     */
    public void stopSending() {
        if (isServiceRunning(BackgroundLocationService.class)) {
            ComponentName comp = new ComponentName(getApplicationContext().getPackageName(), BackgroundLocationService.class.getName());
            getApplicationContext().stopService(new Intent().setComponent(comp));

            TextView dateText = (TextView) findViewById(R.id.text_position_date);
            dateText.setText(null);

            TextView latText = (TextView) findViewById(R.id.text_position_lat);
            latText.setText(null);

            TextView lonText = (TextView) findViewById(R.id.text_position_lon);
            lonText.setText(null);

            TextView altText = (TextView) findViewById(R.id.text_position_alt);
            altText.setText(null);

            TextView speedText = (TextView) findViewById(R.id.text_position_speed);
            speedText.setText(null);

            TextView speedBearing = (TextView) findViewById(R.id.text_position_bearing);
            speedBearing.setText(null);

            TextView respText = (TextView) findViewById(R.id.text_http_response);
            respText.setText(null);

            //remove flag, if any
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;
            }
        } else
            Toast.makeText(this, R.string.localiz_not_run, Toast.LENGTH_SHORT).show();
    }

    /**
     * Check service is running
     * http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
     *
     * @param serviceClass
     * @return
     */
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * This class uses the BroadcastReceiver framework to detect and handle new postition messages from
     * the service
     */
    private class NewPositionReceiver extends BroadcastReceiver {
        // prevents instantiation by other packages.
        private NewPositionReceiver() {
        }

        /**
         * This method is called by the system when a broadcast Intent is matched by this class'
         * intent filters
         *
         * @param context
         * @param intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Location lastLocation = (Location) intent.getExtras().get(Constants.EXTRAS_POSITION_DATA);
            mLastServerResponse = intent.getStringExtra(Constants.EXTRAS_SERVER_RESPONSE);
            if (lastLocation != null || mLastServerResponse != null) {
                //2014-06-28T15:07:59
                mLastLocationTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(lastLocation.getTime());
                mLastLocationLat = Double.toString(lastLocation.getLatitude());
                mLastLocationLon = Double.toString(lastLocation.getLongitude());
                mLastLocationAlt = String.valueOf(lastLocation.getAltitude());
                mLastLocationSpeed = String.valueOf(lastLocation.getSpeed());
                mLastLocationBearing = String.valueOf(lastLocation.getBearing());
                UpdateGUI();
            }
        }
    }

    private void UpdateGUI() {
        TextView dateText = (TextView) findViewById(R.id.text_position_date);
        dateText.setText(mLastLocationTime);

        TextView latText = (TextView) findViewById(R.id.text_position_lat);
        latText.setText(mLastLocationLat);

        TextView lonText = (TextView) findViewById(R.id.text_position_lon);
        lonText.setText(mLastLocationLon);

        TextView altText = (TextView) findViewById(R.id.text_position_alt);
        altText.setText(mLastLocationAlt);

        TextView speedText = (TextView) findViewById(R.id.text_position_speed);
        speedText.setText(mLastLocationSpeed);

        TextView speedBearing = (TextView) findViewById(R.id.text_position_bearing);
        speedBearing.setText(mLastLocationBearing);

        TextView httpRespText = (TextView) findViewById(R.id.text_http_response);
        httpRespText.setText(mLastServerResponse);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save the user's current state
        savedInstanceState.putString(STATE_LAST_LOCATION_TIME, mLastLocationTime);
        savedInstanceState.putString(STATE_LAST_LOCATION_LAT, mLastLocationLat);
        savedInstanceState.putString(STATE_LAST_LOCATION_LON, mLastLocationLon);
        savedInstanceState.putString(STATE_LAST_LOCATION_ALT, mLastLocationAlt);
        savedInstanceState.putString(STATE_LAST_LOCATION_SPEED, mLastLocationSpeed);
        savedInstanceState.putString(STATE_LAST_LOCATION_BEARING, mLastLocationBearing);
        savedInstanceState.putString(STATE_LAST_SERVER_RESPONSE, mLastServerResponse);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore state members from saved instance
        mLastLocationTime = savedInstanceState.getString(STATE_LAST_LOCATION_TIME);
        mLastLocationLat = savedInstanceState.getString(STATE_LAST_LOCATION_LAT);
        mLastLocationLon = savedInstanceState.getString(STATE_LAST_LOCATION_LON);
        mLastLocationAlt = savedInstanceState.getString(STATE_LAST_LOCATION_ALT);
        mLastLocationSpeed = savedInstanceState.getString(STATE_LAST_LOCATION_SPEED);
        mLastLocationBearing = savedInstanceState.getString(STATE_LAST_LOCATION_BEARING);
        mLastServerResponse = savedInstanceState.getString(STATE_LAST_SERVER_RESPONSE);

        UpdateGUI();
    }
}

