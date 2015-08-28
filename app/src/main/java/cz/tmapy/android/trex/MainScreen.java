package cz.tmapy.android.trex;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.SimpleDateFormat;

import cz.tmapy.android.trex.update.Updater;

public class MainScreen extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "MainScreen";

    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private String mActivityTitle;

    private PowerManager.WakeLock mWakeLock;

    private String mTargetServerURL;
    private String mDeviceId;
    private Boolean mKeepScreenOn = false;

    //members for state saving
    private Boolean mLocalizationIsRunning = false;
    private String mLastLocationTime;
    private String mLastLocationLat;
    private String mLastLocationLon;
    private String mLastLocationAlt;
    private String mLastLocationSpeed;
    private String mLastLocationBearing;
    private String mLastServerResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        mDrawerList = (ListView)findViewById(R.id.navList);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mActivityTitle = getTitle().toString();

        addDrawerItems();
        setupDrawer();

        final ToggleButton toggle = (ToggleButton) findViewById(R.id.toggle_start);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)  {
                    if (!mLocalizationIsRunning)
                    {
                        Boolean startSuccess  = startSending();
                        if (!startSuccess) //cancel toggle switch when service' start is not successful
                            toggle.setChecked(false);
                    }
                } else stopSending();
            }
        });

        //get intent info from service (if any)
        Boolean locIsRunning = getIntent().getBooleanExtra(Const.STATE_LOCALIZATION_IS_RUNNING, false);
        //pokud bylo v předaném intentu, že lokalizace již běží
        if (locIsRunning)
        {
            mLocalizationIsRunning = locIsRunning; //nastav, že lokalizace již běží
            toggle.setChecked(true); //nastav tlačítko na True
        }

        //Registrace broadcastreceiveru komunikaci se sluzbou (musi byt tady, aby fungoval i po nove inicializaci aplikace z notifikace
        // The filter's action is BROADCAST_ACTION
        IntentFilter mIntentFilter = new IntentFilter(Const.LOCATION_BROADCAST);
        // Instantiates a new mPositionReceiver
        NewPositionReceiver mPositionReceiver = new NewPositionReceiver();
        // Registers the mPositionReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(mPositionReceiver, mIntentFilter);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this); //to get pref changes to onSharePreferenceChanged
        mDeviceId = sharedPref.getString(Const.PREF_KEY_DEVICE_ID, "");
        mTargetServerURL = sharedPref.getString(Const.PREF_KEY_TARGET_SERVUER_URL, "");
        mKeepScreenOn = sharedPref.getBoolean(Const.PREF_KEY_KEEP_SCREEN_ON, false);

        //if this is not orientation change (saved bundle doesn't exists) check for update
        if (savedInstanceState == null && sharedPref.getBoolean("pref_check4update", true))
            new Updater(this).execute();

        //ACRA.getErrorReporter().putCustomData("myKey", "myValue");
        //ACRA.getErrorReporter().handleException(new Exception("Test exception"));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void addDrawerItems() {
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.drawer_menu));
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Highlight the selected item
                mDrawerList.setItemChecked(position, true);
                switch (position){
                    case 0:
                        Intent intent = new Intent(getApplicationContext(), Settings.class);
                        startActivity(intent);
                        break;
                    case 1:
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Const.HELP_SITE_URL)));
                        break;
                    case 2:
                        new Updater(getApplicationContext()).execute();
                        break;
                    default:
                        Toast.makeText(MainScreen.this, "I'm sorry - not implemented!", Toast.LENGTH_SHORT).show();
                        break;
                }
                //and close the drawer
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        });
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(R.string.drawer_title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mActivityTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        // Aktualizuje lokální proměnné při změně konfigurace
        switch (key) {
            case Const.PREF_KEY_DEVICE_ID:
                mDeviceId = prefs.getString(key, "");
                break;
            case Const.PREF_KEY_TARGET_SERVUER_URL:
                mTargetServerURL = prefs.getString(key, "");
                break;
            case Const.PREF_KEY_KEEP_SCREEN_ON:
                mKeepScreenOn = prefs.getBoolean(key, false);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the drawer toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
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
                        if (Const.LOG_BASIC)
                            Log.e(TAG, "Could not start localization service " + comp.toString());
                        mLocalizationIsRunning = false;
                        return false;
                    } else
                    {
                        mLocalizationIsRunning = true;
                        return true;
                    }
                } else {
                    Toast.makeText(this, R.string.localiz_run, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.set_device_id, Toast.LENGTH_LONG).show();
                if (Const.LOG_BASIC) Log.e(TAG, "Device identifier is not setted");
            }
        } else {
            Toast.makeText(this, R.string.set_target_url, Toast.LENGTH_LONG).show();
            if (Const.LOG_BASIC) Log.e(TAG, "Target server URL is not setted");
        }
        mLocalizationIsRunning = false;
        return false;
    }

    /**
     * Switch off sending
     */
    public void stopSending() {
        if (isServiceRunning(BackgroundLocationService.class)) {
            ComponentName comp = new ComponentName(getApplicationContext().getPackageName(), BackgroundLocationService.class.getName());
            getApplicationContext().stopService(new Intent().setComponent(comp));
            mLocalizationIsRunning = false;

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
            Location newLocation = (Location) intent.getExtras().get(Const.EXTRAS_POSITION_DATA);
            mLastServerResponse = intent.getStringExtra(Const.EXTRAS_SERVER_RESPONSE);
            if (newLocation != null || mLastServerResponse != null) {
                //2014-06-28T15:07:59
                mLastLocationTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(newLocation.getTime());
                mLastLocationLat = Double.toString(newLocation.getLatitude());
                mLastLocationLon = Double.toString(newLocation.getLongitude());
                mLastLocationAlt = String.valueOf(newLocation.getAltitude());
                mLastLocationSpeed = String.valueOf(newLocation.getSpeed());
                mLastLocationBearing = String.valueOf(newLocation.getBearing());
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
        savedInstanceState.putBoolean(Const.STATE_LOCALIZATION_IS_RUNNING, mLocalizationIsRunning);
        savedInstanceState.putString(Const.STATE_LAST_LOCATION_TIME, mLastLocationTime);
        savedInstanceState.putString(Const.STATE_LAST_LOCATION_LAT, mLastLocationLat);
        savedInstanceState.putString(Const.STATE_LAST_LOCATION_LON, mLastLocationLon);
        savedInstanceState.putString(Const.STATE_LAST_LOCATION_ALT, mLastLocationAlt);
        savedInstanceState.putString(Const.STATE_LAST_LOCATION_SPEED, mLastLocationSpeed);
        savedInstanceState.putString(Const.STATE_LAST_LOCATION_BEARING, mLastLocationBearing);
        savedInstanceState.putString(Const.STATE_LAST_SERVER_RESPONSE, mLastServerResponse);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore state members from saved instance
        mLocalizationIsRunning = savedInstanceState.getBoolean(Const.STATE_LOCALIZATION_IS_RUNNING);
        mLastLocationTime = savedInstanceState.getString(Const.STATE_LAST_LOCATION_TIME);
        mLastLocationLat = savedInstanceState.getString(Const.STATE_LAST_LOCATION_LAT);
        mLastLocationLon = savedInstanceState.getString(Const.STATE_LAST_LOCATION_LON);
        mLastLocationAlt = savedInstanceState.getString(Const.STATE_LAST_LOCATION_ALT);
        mLastLocationSpeed = savedInstanceState.getString(Const.STATE_LAST_LOCATION_SPEED);
        mLastLocationBearing = savedInstanceState.getString(Const.STATE_LAST_LOCATION_BEARING);
        mLastServerResponse = savedInstanceState.getString(Const.STATE_LAST_SERVER_RESPONSE);
        UpdateGUI();

        super.onRestoreInstanceState(savedInstanceState); //restore after set mLocalizationIsRunning (because of button state)
    }
}

