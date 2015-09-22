package cz.tmapy.android.trex;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.acra.ACRA;

import java.text.SimpleDateFormat;

import cz.tmapy.android.trex.database.DatabaseManager;
import cz.tmapy.android.trex.database.TrackDataSource;
import cz.tmapy.android.trex.database.dobs.TrackDob;
import cz.tmapy.android.trex.drawer.DrawerItemCustomAdapter;
import cz.tmapy.android.trex.drawer.ObjectDrawerItem;
import cz.tmapy.android.trex.layout.TrackDataCursorAdapter;
import cz.tmapy.android.trex.service.BackgroundLocationService;
import cz.tmapy.android.trex.update.Updater;

public class MainScreen extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainScreen.class.getName();

    private IntentFilter mIntentFilter;
    private NewPositionReceiver mPositionReceiver;

    private TrackDataSource mTrackDataSource;
    private TrackDataCursorAdapter mTrackDataCursorAdapter;
    private ListView mTracksListView;

    private SharedPreferences sharedPref;
    private String[] mNavigationDrawerItemTitles;
    private ListView mNavigationDrawerList;
    private DrawerLayout mNavigationDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mActivityTitle;
    private String mTargetServerURL;
    private String mDeviceId;
    private Boolean mKeepScreenOn;
    //members for state saving
    private Boolean mLocalizationIsRunning = false;
    private String mLastLocationTime;
    private String mAccuracy;
    private String mLastLocationAlt;
    private String mLastLocationSpeed;
    private String mAddress;
    private String mLastServerResponse;
    private String mDistance;
    private String mDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        if (!checkPlayServices()) return;

        //google play services available, hooray
        DatabaseManager.init(this);
        mTrackDataSource = new TrackDataSource();
        mTracksListView = (ListView) findViewById(R.id.list_view);
        mTracksListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                AlertDialog.Builder ad = new AlertDialog.Builder(MainScreen.this);
                ad.setTitle(R.string.dialog_delete_title);
                ad.setMessage(R.string.dialog_delete_message);
                final int positionToRemove = pos;
                ad.setNegativeButton(android.R.string.no, null);
                ad.setPositiveButton(android.R.string.yes, new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Cursor c = (Cursor) mTrackDataCursorAdapter.getItem(positionToRemove);
                        mTrackDataSource.deleteTrack(c.getLong(0));
                        reloadTracks();
                    }
                });
                ad.show();
                return true;
            }
        });

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this); //to get pref changes to onSharePreferenceChanged
        mDeviceId = sharedPref.getString(Const.PREF_KEY_DEVICE_ID, "");
        mTargetServerURL = sharedPref.getString(Const.PREF_KEY_TARGET_SERVUER_URL, "");
        mKeepScreenOn = sharedPref.getBoolean(Const.PREF_KEY_KEEP_SCREEN_ON, false);
        HandleKeepScreenOn();

        mNavigationDrawerItemTitles = getResources().getStringArray(R.array.drawer_menu);
        mNavigationDrawerList = (ListView) findViewById(R.id.navList);
        mNavigationDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mActivityTitle = getTitle().toString();

        addDrawerItems();
        setupDrawer();

        final FloatingActionButton startButton = (FloatingActionButton) findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mLocalizationIsRunning) {
                    Boolean startSuccess = startSending();
                    if (startSuccess)
                        startButton.setImageResource(R.drawable.ic_pause_white_36dp);
                } else {
                    stopSending();
                    startButton.setImageResource(R.drawable.ic_play_arrow_white_36dp);
                }
            }
        });

        mLocalizationIsRunning = isServiceRunning(BackgroundLocationService.class);
        if (mLocalizationIsRunning) {
            ReloadLastPosition();
            startButton.setImageResource(R.drawable.ic_pause_white_36dp);
        }

        // 1) localization is not running - activity was not executed from service
        // 2) savedInstanceState is null - it is not change of device orientation (saved bundle doesn't exists)
        // 3) automatic check for update is enabled
        if (!mLocalizationIsRunning && savedInstanceState == null && sharedPref.getBoolean("pref_check4update", true))
            new Updater(MainScreen.this).execute();

        //ACRA.getErrorReporter().putCustomData("myKey", "myValue");
        //ACRA.getErrorReporter().handleException(new Exception("Test exception"));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    /**
     * Check for google play services availability
     * @return
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int errorCheck = api.isGooglePlayServicesAvailable(this);
        if (errorCheck == ConnectionResult.SUCCESS) {
            //google play services available, hooray
            return true;
        } else if (api.isUserResolvableError(errorCheck)) {
            //GPS_REQUEST_CODE = 1000, and is used in onActivityResult
            api.showErrorDialogFragment(this, errorCheck, 1111);
            //stop our activity initialization code
        }
        Log.e(TAG, "Google play services not available");
        return false;
    }

    /**
     * Check service is running
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

    private void addDrawerItems() {

        ObjectDrawerItem[] drawerItem = new ObjectDrawerItem[4];

        drawerItem[0] = new ObjectDrawerItem(R.drawable.ic_settings_black_24dp, mNavigationDrawerItemTitles[0]);
        drawerItem[1] = new ObjectDrawerItem(R.drawable.ic_autorenew_black_24dp, mNavigationDrawerItemTitles[1]);
        drawerItem[2] = new ObjectDrawerItem(R.drawable.ic_help_outline_black_24dp, mNavigationDrawerItemTitles[2]);
        drawerItem[3] = new ObjectDrawerItem(R.drawable.ic_info_outline_black_24dp, mNavigationDrawerItemTitles[3]);

        DrawerItemCustomAdapter adapter = new DrawerItemCustomAdapter(this, R.layout.menu_listview_row, drawerItem);
        mNavigationDrawerList.setAdapter(adapter);

        mNavigationDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Highlight the selected item
                mNavigationDrawerList.setItemChecked(position, true);
                DrawerItemClick(position);
                mNavigationDrawerList.setSelection(position);
                //and close the drawer
                mNavigationDrawerLayout.closeDrawer(mNavigationDrawerList);
            }
        });
    }

    /**
     * Handle clik on navigation drawer item
     *
     * @param position
     */
    private void DrawerItemClick(int position) {
        switch (position) {
            case 0:
                Intent intent = new Intent(getApplicationContext(), Settings.class);
                startActivity(intent);
                break;
            case 1:
                new Updater(MainScreen.this).execute();
                break;
            case 2:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Const.HELP_SITE_URL)));
                break;
            case 3:
                showAbout();
                break;
            default:
                Toast.makeText(MainScreen.this, "I'm sorry - not implemented!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * Shows about dialog
     */
    protected void showAbout() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.about_dialog, null, false);

        messageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new AlertDialog.Builder(MainScreen.this)
                        .setTitle(R.string.test_exception)
                        .setMessage(R.string.test_exception_message)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ACRA.getErrorReporter().handleException(new Exception("TEST EXCEPTION"));
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }
        });

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.drawable.trainers);
            builder.setTitle(getResources().getString(R.string.app_name) + " " + pInfo.versionName);
            builder.setView(messageView);
            builder.create();
            builder.show();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
        }
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mNavigationDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

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
        mNavigationDrawerLayout.setDrawerListener(mDrawerToggle);
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
                HandleKeepScreenOn();
                break;
        }
    }

    /**
     * Handle "keep screen on" flag
     */
    private void HandleKeepScreenOn() {
        if (mKeepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else
            //remove flag, if any
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                if (!mLocalizationIsRunning) {

                    //Nastartovani sluzby
                    ComponentName comp = new ComponentName(getApplicationContext().getPackageName(), BackgroundLocationService.class.getName());
                    ComponentName service = getApplicationContext().startService(new Intent().setComponent(comp));

                    if (null != service) {
                        resetGUI();
                        mLocalizationIsRunning = true;
                        return true;
                    }

                    // something really wrong here
                    Toast.makeText(this, R.string.localiz_could_not_start, Toast.LENGTH_SHORT).show();
                    if (Const.LOG_BASIC)
                        Log.e(TAG, "Could not start localization service " + comp.toString());

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
        if (mLocalizationIsRunning) {
            ComponentName comp = new ComponentName(getApplicationContext().getPackageName(), BackgroundLocationService.class.getName());
            getApplicationContext().stopService(new Intent().setComponent(comp));
        } else
            Toast.makeText(this, R.string.localiz_not_run, Toast.LENGTH_SHORT).show();

        mLocalizationIsRunning = false;
    }

    private void resetGUI() {
        TextView dateText = (TextView) findViewById(R.id.text_position_date);
        dateText.setText(null);

        TextView durationText = (TextView) findViewById(R.id.text_duration);
        durationText.setText(null);

        TextView accuText = (TextView) findViewById(R.id.text_position_accuracy);
        accuText.setText(null);

        TextView distText = (TextView) findViewById(R.id.text_distance);
        distText.setText(null);

        TextView altText = (TextView) findViewById(R.id.text_position_alt);
        altText.setText(null);

        TextView speedText = (TextView) findViewById(R.id.text_position_speed);
        speedText.setText(null);

        TextView addressText = (TextView) findViewById(R.id.text_address);
        addressText.setText(null);

        TextView respText = (TextView) findViewById(R.id.text_http_response);
        respText.setText(null);
    }

    private void UpdateGUI() {

        TextView dateText = (TextView) findViewById(R.id.text_position_date);
        dateText.setText(mLastLocationTime);

        if (mDuration != null) {
            TextView durationText = (TextView) findViewById(R.id.text_duration);
            durationText.setText(mDuration);
        }

        if (mAccuracy != null) {
            TextView accuracy = (TextView) findViewById(R.id.text_position_accuracy);
            accuracy.setText(mAccuracy + " m");
        }

        if (mDistance != null) {
            TextView estDist = (TextView) findViewById(R.id.text_distance);
            estDist.setText(mDistance + " km");
        }

        if (mLastLocationSpeed != null) {
            TextView speedText = (TextView) findViewById(R.id.text_position_speed);
            speedText.setText(mLastLocationSpeed + " km/h");
        }

        if (mLastLocationAlt != null) {
            TextView altText = (TextView) findViewById(R.id.text_position_alt);
            altText.setText(mLastLocationAlt + " m");
        }

        if (mAddress != null) {
            TextView address = (TextView) findViewById(R.id.text_address);
            address.setText(mAddress);
        }
        if (mLastServerResponse != null) {
            TextView httpRespText = (TextView) findViewById(R.id.text_http_response);
            httpRespText.setText(mLastServerResponse);
        }
    }

    @Override
    protected void onPause() {
        mTracksListView.setAdapter(null);
        mTrackDataCursorAdapter = null;
        DatabaseManager.deactivate();
        // Musí se odregistrovat receiver, jinak se bude volat vícekrát (podle počtu, kolikrát přešel do pause a zpět)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPositionReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseManager.init(this);
        mTrackDataCursorAdapter = new TrackDataCursorAdapter(MainScreen.this, mTrackDataSource.getAllTracksCursor(), 0);
        mTracksListView.setAdapter(mTrackDataCursorAdapter);

        //Registrace broadcastreceiveru komunikaci se sluzbou (musi byt tady, aby fungoval i po nove inicializaci aplikace z notifikace
        mIntentFilter = new IntentFilter(Const.LOCATION_BROADCAST);
        mPositionReceiver = new NewPositionReceiver();
        // Registers the mPositionReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(mPositionReceiver, mIntentFilter);

        if (mLocalizationIsRunning) {
            ReloadLastPosition();
        }
    }

    /**
     * Load last state from preferences
     */
    public void ReloadLastPosition() {

        mLastLocationTime = new SimpleDateFormat("HH:mm:ss").format(sharedPref.getLong(Const.LOCATION_TIME, 0));
        mLastLocationAlt = String.format("%.0f", Double.longBitsToDouble(sharedPref.getLong(Const.ALTITUDE, 0))); //http://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
        mLastLocationSpeed = String.format("%.0f", (sharedPref.getFloat(Const.SPEED, 0) / 1000) * 3600);
        mAccuracy = String.format("%.1f", sharedPref.getFloat(Const.ACCURACY, 0));

        Long d = sharedPref.getLong(Const.DURATION, 0l) / 1000;
        mDuration = String.format("%d:%02d:%02d", d / 3600, (d % 3600) / 60, (d % 60));

        mDistance = String.format("%.2f", (sharedPref.getFloat(Const.DISTANCE, 0.0f) / 1000));

        mAddress = sharedPref.getString(Const.ADDRESS, "");
        mLastServerResponse = sharedPref.getString(Const.SERVER_RESPONSE, "");

        UpdateGUI();
    }

    /**
     * Load tracks form database
     */
    private void reloadTracks() {
        if (null != mTrackDataCursorAdapter) {
            mTrackDataCursorAdapter.swapCursor(mTrackDataSource.getAllTracksCursor());
            mTrackDataCursorAdapter.notifyDataSetChanged();
        }
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
            Location location = (Location) intent.getExtras().get(Const.POSITION);
            if (location != null) {
                //2014-06-28T15:07:59
                mLastLocationTime = new SimpleDateFormat("HH:mm:ss").format(location.getTime());
                mLastLocationAlt = String.format("%.0f", location.getAltitude());
                mLastLocationSpeed = String.format("%.0f", (location.getSpeed() / 1000) * 3600);
                mAccuracy = String.format("%.1f", location.getAccuracy());
            }
            if (intent.hasExtra(Const.ADDRESS)) {
                String adr = intent.getStringExtra(Const.ADDRESS);
                if (adr != null)
                    mAddress = adr;
            }

            if (intent.hasExtra(Const.DURATION)) {
                Long d = intent.getLongExtra(Const.DURATION, 0l) / 1000;
                mDuration = String.format("%d:%02d:%02d", d / 3600, (d % 3600) / 60, (d % 60));
            }

            if (intent.hasExtra(Const.DISTANCE))
                mDistance = String.format("%.2f", (intent.getFloatExtra(Const.DISTANCE, 0.0f) / 1000));

            mLastServerResponse = intent.getStringExtra(Const.SERVER_RESPONSE);
            UpdateGUI();

            //if this is final broadcast
            if (intent.hasExtra(Const.FINISH_TIME)) {
                TrackDob trackDob = new TrackDob();
                trackDob.setStartTime(intent.getLongExtra(Const.START_TIME, 0l));
                trackDob.setStartLat(intent.getDoubleExtra(Const.START_LAT, 0d));
                trackDob.setStartLon(intent.getDoubleExtra(Const.START_LON, 0d));
                trackDob.setStartAddress(intent.getStringExtra(Const.START_ADDRESS));
                trackDob.setFinishTime(intent.getLongExtra(Const.FINISH_TIME, 0l));
                trackDob.setFinishLat(intent.getDoubleExtra(Const.FINISH_LAT, 0d));
                trackDob.setFinishLon(intent.getDoubleExtra(Const.FINISH_LON, 0d));
                trackDob.setFinishAddress(intent.getStringExtra(Const.FINISH_ADDRESS));
                trackDob.setDistance(intent.getFloatExtra(Const.DISTANCE, 0f));
                trackDob.setMaxSpeed(intent.getFloatExtra(Const.MAX_SPEED, 0f));
                trackDob.setAveSpeed(intent.getFloatExtra(Const.AVE_SPEED, 0f));
                trackDob.setMinAlt(intent.getDoubleExtra(Const.MIN_ALT, 0d));
                trackDob.setMaxAlt(intent.getDoubleExtra(Const.MAX_ALT, 0d));
                trackDob.setElevDiffUp(intent.getDoubleExtra(Const.ELEV_DIFF_UP, 0d));
                trackDob.setElevDiffDown(intent.getDoubleExtra(Const.ELEV_DIFF_DOWN, 0d));
                trackDob.setNote(mLastLocationTime);

                mTrackDataSource.saveTrack(trackDob);
                reloadTracks();
            }
        }
    }
}

