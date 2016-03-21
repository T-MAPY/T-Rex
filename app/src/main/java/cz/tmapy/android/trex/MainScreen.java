package cz.tmapy.android.trex;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
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
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.acra.ACRA;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import cz.tmapy.android.trex.database.DatabaseManager;
import cz.tmapy.android.trex.database.TrackDataSource;
import cz.tmapy.android.trex.database.dobs.TrackDob;
import cz.tmapy.android.trex.layout.TrackDataCursorAdapter;
import cz.tmapy.android.trex.layout.TrackTypeArrayAdapter;
import cz.tmapy.android.trex.service.BackgroundLocationService;
import cz.tmapy.android.trex.service.ServiceHelper;
import cz.tmapy.android.trex.update.Updater;

public class MainScreen extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainScreen.class.getName();

    //https://guides.codepath.com/android/Fragment-Navigation-Drawer
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;
    private ActionBarDrawerToggle drawerToggle;

    private IntentFilter mIntentFilter;
    private NewPositionReceiver mPositionReceiver;

    private TrackDataSource mTrackDataSource;
    private TrackDataCursorAdapter mTrackDataCursorAdapter;
    private ListView mTracksListView;

    private SharedPreferences sharedPref;

    private String mTargetServerURL;
    private String mDeviceId;
    private Boolean mKeepScreenOn;
    private Boolean mSendTag;
    //members for state saving
    private Boolean mLocalizationIsRunning = false;
    private String mTag;
    private Long mStartTime;
    private Long mLastLocationTime;
    private String mLastLocationTimeString;
    private String mAccuracyString;
    private String mLastLocationAltString;
    private String mLastLocationSpeedString;
    private String mAddressString;
    private String mLastServerResponseString;
    private String mDistanceString;
    private String mDurationString;
    private Integer mKeepNumberOfTracks;

    //Permissions
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set a Toolbar to replace the ActionBar. In order to slide our navigation drawer over the ActionBar,
        // we need to use the new Toolbar widget as defined in the AppCompat v21 library.
        // The Toolbar can be embedded into your view hierarchy
        // which makes sure that the drawer slides over the ActionBar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Find our drawer view
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        // Find our drawer view
        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        // Setup drawer view
        setupDrawerContent(nvDrawer);

        drawerToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close);
        // Tie DrawerLayout events to the ActionBarToggle
        mDrawer.setDrawerListener(drawerToggle);

        if (!ServiceHelper.checkPlayServices(MainScreen.this)) return;

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
        mKeepNumberOfTracks = Integer.valueOf(sharedPref.getString(Const.PREF_KEY_KEEP_NUMBER_OF_TRACKS, "30"));

        mKeepScreenOn = sharedPref.getBoolean(Const.PREF_KEY_KEEP_SCREEN_ON, false);
        HandleKeepScreenOn();

        mTag = sharedPref.getString(Const.PREF_KEY_TAG, getString(R.string.default_tag));
        mSendTag = sharedPref.getBoolean(Const.PREF_KEY_SEND_TAG, false);
        final int tagVisibility = mSendTag ? View.VISIBLE : View.GONE;
        final TextView tagLabel = (TextView) findViewById(R.id.tag_label);
        tagLabel.setVisibility(tagVisibility);
        final Button tagButton = (Button) findViewById(R.id.tag_button);
        if (tagButton != null) {
            tagButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // custom dialog
                    final Dialog dialog = new Dialog(MainScreen.this);
                    dialog.setContentView(R.layout.select_track_type_dialog);
                    dialog.setTitle("Vyberte prosím typ trasy");

                    final EditText editText = (EditText) dialog.findViewById(R.id.selectTrackTypeDialogText);

                    Button dialogButton = (Button) dialog.findViewById(R.id.selectTrackTypeDialogButtonOK);
                    // if button is clicked, close the custom dialog
                    dialogButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mTag = editText.getText().toString();
                            tagButton.setText(mTag);
                            sharedPref.edit().putString(Const.PREF_KEY_TAG, mTag).apply();
                            dialog.dismiss();
                        }
                    });

                    final ListView tagListView = (ListView) dialog.findViewById(R.id.selectTrackTypeList);
                    final ArrayList<String> tagList = new ArrayList<String>();
                    tagList.add("výlet");
                    tagList.add("výjezd");
                    tagList.add("cesta do práce");
                    String[] mStringArray = new String[tagList.size()];
                    mStringArray = tagList.toArray(mStringArray);

                    final TrackTypeArrayAdapter adapter = new TrackTypeArrayAdapter(MainScreen.this, mStringArray);
                    tagListView.setAdapter(adapter);
                    tagListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            mTag = tagList.get(position).toString();  //Selected item in listview
                            tagButton.setText(mTag);
                            sharedPref.edit().putString(Const.PREF_KEY_TAG, mTag).apply();
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                }
            });
            tagButton.setText(mTag);
            tagButton.setVisibility(tagVisibility);
        }

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

        mLocalizationIsRunning = ServiceHelper.isServiceRunning(BackgroundLocationService.class, MainScreen.this);
        if (mLocalizationIsRunning) {
            ReloadLastPosition();
            startButton.setImageResource(R.drawable.ic_pause_white_36dp);
        }

        // 1) localization is not running - activity was not executed from service
        // 2) savedInstanceState is null - it is not change of device orientation (saved bundle doesn't exists)
        // 3) automatic check for update is enabled
        if (!mLocalizationIsRunning && savedInstanceState == null && sharedPref.getBoolean("pref_check4update", true)) {
            new Updater(MainScreen.this).execute();
        }

        //ACRA.getErrorReporter().putCustomData("myKey", "myValue");
        //ACRA.getErrorReporter().handleException(new Exception("Test exception"));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    /**
     * Start localizing and sending
     */
    public Boolean startSending() {
        if (!mTargetServerURL.isEmpty()) {
            if (!mDeviceId.isEmpty()) {
                if (!mLocalizationIsRunning) {
                    if (ContextCompat.checkSelfPermission(MainScreen.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (CheckProviders()) {
                            //Nastartovani sluzby
                            ComponentName comp = new ComponentName(getApplicationContext().getPackageName(), BackgroundLocationService.class.getName());
                            ComponentName service = getApplicationContext().startService(new Intent().setComponent(comp));

                            if (null != service) {
                                clearLastState();
                                UpdateGUI();
                                mLocalizationIsRunning = true;
                                mStartTime = System.currentTimeMillis();
                                return true;
                            }

                            // something really wrong here
                            Toast.makeText(this, R.string.localiz_could_not_start, Toast.LENGTH_SHORT).show();
                            if (Const.LOG_BASIC)
                                Log.e(TAG, "Could not start localization service " + comp.toString());
                        }
                    } else {
                        // Should we show an explanation?
                        if (ActivityCompat.shouldShowRequestPermissionRationale(MainScreen.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                            Toast.makeText(MainScreen.this, getResources().getString(R.string.perm_localiz_right_needed), Toast.LENGTH_LONG).show();
                            ActivityCompat.requestPermissions(MainScreen.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                        } else
                            ActivityCompat.requestPermissions(MainScreen.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
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
        if (mLocalizationIsRunning) {
            ComponentName comp = new ComponentName(getApplicationContext().getPackageName(), BackgroundLocationService.class.getName());
            getApplicationContext().stopService(new Intent().setComponent(comp));
        } else
            Toast.makeText(this, R.string.localiz_not_run, Toast.LENGTH_SHORT).show();

        mLocalizationIsRunning = false;
    }

    /**
     * Check the location provider is enabled and airplane mode disabled
     *
     * @return
     */
    protected boolean CheckProviders() {
        boolean airplane_mode = true;
        LocationManager lm = (LocationManager) MainScreen.this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            airplane_mode = android.provider.Settings.System.getInt(MainScreen.this.getContentResolver(),
                    android.provider.Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            airplane_mode = android.provider.Settings.Global.getInt(MainScreen.this.getContentResolver(),
                    android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }

        if (airplane_mode) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainScreen.this);
            dialog.setTitle(getResources().getString(R.string.cannot_start_localization));
            //dialog.setIcon(getResources().getDrawable(R.drawable.ic_info_outline_black_24dp));
            dialog.setMessage(getResources().getString(R.string.airplane_mode_enabled));
            dialog.setPositiveButton(getResources().getString(R.string.airplane_open_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        try {
                            Intent intentAirplaneMode = new Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                            intentAirplaneMode.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentAirplaneMode);
                        } catch (ActivityNotFoundException e) {
                            Log.e(TAG, "ActivityNotFoundException", e);
                        }
                    } else {
                        Intent intent1 = new Intent("android.settings.WIRELESS_SETTINGS");
                        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent1);
                    }
                }
            });
            dialog.setNegativeButton(MainScreen.this.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Toast.makeText(MainScreen.this, getResources().getString(R.string.cannot_start_localization), Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show();
            return false;
        }

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            Log.e(TAG, "Get GPS status exception", ex);
        }

        if (!gps_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainScreen.this);
            dialog.setTitle(getResources().getString(R.string.cannot_start_localization));
            dialog.setMessage(getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(getResources().getString(R.string.gps_open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    MainScreen.this.startActivity(myIntent);
                }
            });
            dialog.setNegativeButton(MainScreen.this.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Toast.makeText(MainScreen.this, getResources().getString(R.string.cannot_start_localization), Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show();
            return false;
        }
        return true;
    }

    //region ************ TRACKS AND POSITIONS ***********

    /**
     * Load last state from preferences
     */
    public void ReloadLastPosition() {

        mStartTime = sharedPref.getLong(Const.START_TIME, 0);
        mLastLocationTime = sharedPref.getLong(Const.LAST_LOCATION_TIME, 0);

        mLastLocationTimeString = new SimpleDateFormat("HH:mm:ss").format(mLastLocationTime);
        mLastLocationAltString = String.format("%.0f", Double.longBitsToDouble(sharedPref.getLong(Const.ALTITUDE, 0))); //http://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
        mLastLocationSpeedString = String.format("%.0f", (sharedPref.getFloat(Const.SPEED, 0) / 1000) * 3600);
        mAccuracyString = String.format("%.1f", sharedPref.getFloat(Const.ACCURACY, 0));

        Long d = (System.currentTimeMillis() - mStartTime) / 1000;
        mDurationString = String.format("%d:%02d:%02d", d / 3600, (d % 3600) / 60, (d % 60));

        mDistanceString = String.format("%.2f", (sharedPref.getFloat(Const.DISTANCE, 0.0f) / 1000));

        mAddressString = sharedPref.getString(Const.ADDRESS, "");
        mLastServerResponseString = sharedPref.getString(Const.SERVER_RESPONSE, "");

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
     * Save track info into db
     *
     * @param t
     */
    private void SaveTrack(TrackDob t) {
        mTrackDataSource.saveTrack(t);
        Cursor cursor = mTrackDataSource.getAllTracksCursor();
        int count = cursor.getCount();
        cursor.close();
        if (count > mKeepNumberOfTracks) {
            mTrackDataSource.keepOnlyLastTracks(mKeepNumberOfTracks);
        }
    }

    //endregion

    //region ********* PERMISSIONS *********

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResults) {
        if (grantResults != null && grantResults.length > 0)
            try {
                switch (requestCode) {
                    case MY_PERMISSIONS_REQUEST_FINE_LOCATION:
                        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                            Boolean startSuccess = startSending();
                            if (startSuccess) {
                                final FloatingActionButton startButton = (FloatingActionButton) findViewById(R.id.start_button);
                                startButton.setImageResource(R.drawable.ic_pause_white_36dp);
                            }
                        } else {
                            Toast.makeText(this, getResources().getString(R.string.perm_localiz_not_granted), Toast.LENGTH_SHORT).show();
                        }
                    default:
                        super.onRequestPermissionsResult(requestCode, permission, grantResults);
                        break;
                }
            } catch (SecurityException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
    }

    //endregion

    //region ********** PREFERENCES *************

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
            case Const.PREF_KEY_KEEP_NUMBER_OF_TRACKS:
                mKeepNumberOfTracks = Integer.valueOf(prefs.getString(Const.PREF_KEY_KEEP_NUMBER_OF_TRACKS, "30"));
                break;
            case Const.PREF_KEY_KEEP_SCREEN_ON:
                mKeepScreenOn = prefs.getBoolean(key, false);
                HandleKeepScreenOn();
                break;
            case Const.PREF_KEY_SEND_TAG:
                mSendTag = prefs.getBoolean(key, false);
                final int tagVisibility = mSendTag ? View.VISIBLE : View.GONE;
                final TextView tagLabel = (TextView) findViewById(R.id.tag_label);
                tagLabel.setVisibility(tagVisibility);
                final Button tagButton = (Button) findViewById(R.id.tag_button);
                tagButton.setVisibility(tagVisibility);
                break;
        }
    }

    /**
     * Clear last state in preferences
     */
    public void clearLastState() {

        mStartTime = Long.valueOf(0);
        sharedPref.edit().putLong(Const.START_TIME, mStartTime).apply();

        mLastLocationTimeString = getResources().getString(R.string.text_position_date_empty);
        sharedPref.edit().putLong(Const.LAST_LOCATION_TIME, 0).apply();

        mAccuracyString = "0";
        sharedPref.edit().putFloat(Const.ACCURACY, 0).apply();

        mLastLocationAltString = "0";
        sharedPref.edit().putLong(Const.ALTITUDE, 0).apply();

        mLastLocationSpeedString = "0";
        sharedPref.edit().putFloat(Const.SPEED, 0).apply();

        mDistanceString = "0,00";
        sharedPref.edit().putFloat(Const.DISTANCE, 0).apply();

        mAddressString = getResources().getString(R.string.text_address_empty);
        sharedPref.edit().putString(Const.ADDRESS, mAddressString).apply();

        mLastServerResponseString = getResources().getString(R.string.text_http_response_empty);
        sharedPref.edit().putString(Const.SERVER_RESPONSE, mLastServerResponseString).apply();
    }

    //endregion

    //region ********** BROADCAST RECEIVERS **********

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
            Long mCurrentTime = System.currentTimeMillis();
            Location location = (Location) intent.getExtras().get(Const.POSITION);
            if (location != null) {
                //2014-06-28T15:07:59
                mLastLocationTimeString = new SimpleDateFormat("HH:mm:ss").format(location.getTime());
                mLastLocationAltString = String.format("%.0f", location.getAltitude());
                mLastLocationSpeedString = String.format("%.0f", (location.getSpeed() / 1000) * 3600);
                mAccuracyString = String.format("%.1f", location.getAccuracy());
            }
            if (intent.hasExtra(Const.ADDRESS)) {
                String adr = intent.getStringExtra(Const.ADDRESS);
                if (adr != null)
                    mAddressString = adr;
            }

            if (intent.hasExtra(Const.START_TIME)) {
                mStartTime = intent.getLongExtra(Const.START_TIME, 0l);
                Long d = (mCurrentTime - mStartTime) / 1000;
                mDurationString = String.format("%d:%02d:%02d", d / 3600, (d % 3600) / 60, (d % 60));
            }

            if (intent.hasExtra(Const.DISTANCE))
                mDistanceString = String.format("%.2f", (intent.getFloatExtra(Const.DISTANCE, 0.0f) / 1000));

            mLastServerResponseString = intent.getStringExtra(Const.SERVER_RESPONSE);
            UpdateGUI();

            //if this is final broadcast
            if (intent.hasExtra(Const.LAST_LAT)) {
                TrackDob trackDob = new TrackDob();
                if (mSendTag) trackDob.setTag(mTag);
                trackDob.setStartTime(intent.getLongExtra(Const.START_TIME, 0l));
                trackDob.setFirstLat(intent.getDoubleExtra(Const.FIRST_LAT, 0d));
                trackDob.setFirstLon(intent.getDoubleExtra(Const.FIRST_LON, 0d));
                trackDob.setFirstAddress(intent.getStringExtra(Const.FIRST_ADDRESS));
                trackDob.setFinishTime(mCurrentTime);
                trackDob.setLastLat(intent.getDoubleExtra(Const.LAST_LAT, 0d));
                trackDob.setLastLon(intent.getDoubleExtra(Const.LAST_LON, 0d));
                trackDob.setLastAddress(intent.getStringExtra(Const.LAST_ADDRESS));
                trackDob.setDistance(intent.getFloatExtra(Const.DISTANCE, 0f));
                trackDob.setMaxSpeed(intent.getFloatExtra(Const.MAX_SPEED, 0f));
                trackDob.setAveSpeed(intent.getFloatExtra(Const.AVE_SPEED, 0f));
                trackDob.setMinAlt(intent.getDoubleExtra(Const.MIN_ALT, 0d));
                trackDob.setMaxAlt(intent.getDoubleExtra(Const.MAX_ALT, 0d));
                trackDob.setElevDiffUp(intent.getDoubleExtra(Const.ELEV_DIFF_UP, 0d));
                trackDob.setElevDiffDown(intent.getDoubleExtra(Const.ELEV_DIFF_DOWN, 0d));
                trackDob.setNote(mLastLocationTimeString);

                SaveTrack(trackDob);
                reloadTracks();
            }
        }
    }

    //endregion***

    //region *********** GUI ************

    /**
     * Nastavení reakcí na nabídky v navigation drawer
     *
     * @param navigationView
     */
    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.settings_menu_item:
                                Intent intent = new Intent(getApplicationContext(), Settings.class);
                                startActivity(intent);
                                break;
                            case R.id.reload_menu_item:
                                new Updater(MainScreen.this).execute();
                                break;
                            case R.id.help_item:
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Const.HELP_SITE_URL)));
                                break;
                            case R.id.about_item:
                                showAbout();
                                break;
                            default:
                                //Toast.makeText(MainActivity.this, "not implemented", Toast.LENGTH_SHORT).show();
                        }

                        // Highlight the selected item, update the title, and close the drawer
                        menuItem.setChecked(true);
                        //setTitle(menuItem.getTitle());
                        mDrawer.closeDrawers();
                        return true;
                    }
                });
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
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private void UpdateGUI() {

        TextView dateText = (TextView) findViewById(R.id.text_position_date);
        dateText.setText(mLastLocationTimeString);

        if (mDurationString != null) {
            TextView durationText = (TextView) findViewById(R.id.text_duration);
            durationText.setText(mDurationString);
        }

        if (mAccuracyString != null) {
            TextView accuracy = (TextView) findViewById(R.id.text_position_accuracy);
            accuracy.setText(mAccuracyString + " m");
        }

        if (mDistanceString != null) {
            TextView estDist = (TextView) findViewById(R.id.text_distance);
            estDist.setText(mDistanceString + " km");
        }

        if (mLastLocationSpeedString != null) {
            TextView speedText = (TextView) findViewById(R.id.text_position_speed);
            speedText.setText(mLastLocationSpeedString + " km/h");
        }

        if (mLastLocationAltString != null) {
            TextView altText = (TextView) findViewById(R.id.text_position_alt);
            altText.setText(mLastLocationAltString + " m");
        }

        if (mAddressString != null) {
            TextView address = (TextView) findViewById(R.id.text_address);
            address.setText(mAddressString);
        }
        if (mLastServerResponseString != null) {
            TextView httpRespText = (TextView) findViewById(R.id.text_http_response);
            httpRespText.setText(mLastServerResponseString);
        }
    }

    //endregion

    //region ********** ON PAUSE / On RESUME ********

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

    //endregion
}