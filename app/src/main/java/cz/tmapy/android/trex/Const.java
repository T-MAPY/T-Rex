package cz.tmapy.android.trex;

import android.text.StaticLayout;

/**
 * Created by kasvo on 4.8.2015.
 */
public final class Const {

    //http://stackoverflow.com/questions/2018263/how-do-i-enable-disable-log-levels-in-android
    private static int LOGLEVEL = 2;
    public static boolean LOG_BASIC = LOGLEVEL >= 1;
    public static boolean LOG_ENHANCED = LOGLEVEL >= 2;

    public static final String CHECK_FOR_NEW_VER_URL = "http://distrib.tmapy.cz/pub/distrib/t-rex/version.json";
    public static final String UPDATE_SITE_URL = "http://distrib.tmapy.cz/pub/distrib/t-rex/";
    public static final String HELP_SITE_URL = "https://github.com/T-MAPY/T-Rex/wiki";

    //Preferences keys
    public static final String PREF_KEY_DEVICE_ID = "pref_id";
    public static final String PREF_KEY_TARGET_SERVUER_URL = "pref_targetUrl";
    public static final String PREF_KEY_SECURITY_STRING = "pref_securityString";
    public static final String PREF_KEY_KEEP_SCREEN_ON = "pref_screen_on";
    public static final String PREF_LOC_STRATEGY = "pref_loc_strategy";
    public static final String PREF_LOC_FREQUENCY = "pref_loc_frequency";
    public static final String PREF_MIN_DIST = "pref_min_dist";
    public static final String PREF_SEND_INTERVAL = "pref_send_interval";
    public static final String PREF_KALMAN_MPS = "pref_kalman_mps";
    public static final String PREF_GEOCODING = "pref_geocoding";
    public static final String PREF_KEY_KEEP_NUMBER_OF_TRACKS = "pref_keep_number_of_tracks";
    public static final String PREF_STARTONSTARTUP = "pref_startOnStartup";

    //STATE constants
    public static final String LOCATION_BROADCAST = "cz.tmapy.android.trex.LOCATION_BROADCAST";
    public static final String LAST_LOCATION_TIME = "cz.tmapy.android.trex.LAST_LOCATION_TIME";
    public static final String POSITION = "cz.tmapy.android.trex.POSITION";
    public static final String ACCURACY = "cz.tmapy.android.trex.ACCURACY";
    public static final String SPEED = "cz.tmapy.android.trex.SPEED";
    public static final String ALTITUDE = "cz.tmapy.android.trex.ALTITUDE";
    public static final String ADDRESS = "cz.tmapy.android.trex.ADDRESS";
    public static final String SERVER_RESPONSE = "cz.tmapy.android.trex.SERVER_RESPONSE";

    public static final String START_TIME = "cz.tmapy.android.trex.COL_START_TIME";
    public static final String FIRST_LAT = "cz.tmapy.android.trex.COL_FIRST_LAT";
    public static final String FIRST_LON = "cz.tmapy.android.trex.COL_FIRST_LON";
    public static final String FIRST_ADDRESS = "cz.tmapy.android.trex.COL_FIRST_ADDRESS";
    public static final String LAST_LAT = "cz.tmapy.android.trex.COL_LAST_LAT";
    public static final String LAST_LON = "cz.tmapy.android.trex.COL_LAST_LON";
    public static final String LAST_ADDRESS = "cz.tmapy.android.trex.COL_LAST_ADDRESS";
    public static final String DISTANCE = "cz.tmapy.android.trex.COL_DISTANCE";
    public static final String MAX_SPEED = "cz.tmapy.android.trex.COL_MAX_SPEED";
    public static final String AVE_SPEED = "cz.tmapy.android.trex.COL_AVE_SPEED";
    public static final String MIN_ALT = "cz.tmapy.android.trex.MIN_AlT";
    public static final String MAX_ALT = "cz.tmapy.android.trex.COL_MAX_ALT";
    public static final String ELEV_DIFF_UP = "cz.tmapy.android.trex.COL_ELEV_DIFF_UP";
    public static final String ELEV_DIFF_DOWN = "cz.tmapy.android.trex.COL_ELEV_DIFF_DOWN";

}