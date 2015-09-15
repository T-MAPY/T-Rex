package cz.tmapy.android.trex;

import android.text.StaticLayout;

/**
 * Created by kasvo on 4.8.2015.
 */
public final class Const {

    //http://stackoverflow.com/questions/2018263/how-do-i-enable-disable-log-levels-in-android
    private static int LOGLEVEL = 1;
    public static boolean LOG_BASIC = LOGLEVEL >= 1;
    public static boolean LOG_ENHANCED = LOGLEVEL >= 2;

    public static final String CHECK_FOR_NEW_VER_URL = "http://distrib.tmapy.cz/pub/distrib/t-rex/version.json";
    public static final String UPDATE_SITE_URL = "http://distrib.tmapy.cz/pub/distrib/t-rex/";
    public static final String HELP_SITE_URL = "https://github.com/T-MAPY/T-Rex/wiki";

    //Preferences keys
    public static final String PREF_KEY_DEVICE_ID = "pref_id";
    public static final String PREF_KEY_TARGET_SERVUER_URL = "pref_targetUrl";
    public static final String PREF_KEY_KEEP_SCREEN_ON = "pref_screen_on";
    public static final String PREF_LOC_STRATEGY = "pref_loc_strategy";
    public static final String PREF_LOC_FREQUENCY = "pref_loc_frequency";
    public static final String PREF_MIN_DIST = "pref_min_dist";
    public static final String PREF_SEND_INTERVAL = "pref_send_interval";
    public static final String PREF_KALMAN_MPS = "pref_kalman_mps";
    public static final String PREF_GEOCODING = "pref_geocoding";

    //STATE constants
    public static final String LOCATION_BROADCAST = "cz.tmapy.android.trex.LOCATION_BROADCAST";
    public static final String LOCATION_TIME = "cz.tmapy.android.trex.LOCATION_TIME";
    public static final String POSITION = "cz.tmapy.android.trex.POSITION";
    public static final String ACCURACY = "cz.tmapy.android.trex.ACCURACY";
    public static final String SPEED = "cz.tmapy.android.trex.SPEED";
    public static final String ALTITUDE = "cz.tmapy.android.trex.ALTITUDE";
    public static final String ADDRESS = "cz.tmapy.android.trex.ADDRESS";
    public static final String SERVER_RESPONSE = "cz.tmapy.android.trex.SERVER_RESPONSE";

    public static final String START_TIME = "cz.tmapy.android.trex.START_TIME";
    public static final String START_LAT = "cz.tmapy.android.trex.START_LAT";
    public static final String START_LON = "cz.tmapy.android.trex.START_LON";
    public static final String START_ADDRESS = "cz.tmapy.android.trex.START_ADDRESS";
    public static final String FINISH_TIME = "cz.tmapy.android.trex.FINISH_TIME";
    public static final String FINISH_LAT = "cz.tmapy.android.trex.FINISH_LAT";
    public static final String FINISH_LON = "cz.tmapy.android.trex.FINISH_LON";
    public static final String FINISH_ADDRESS = "cz.tmapy.android.trex.FINISH_ADDRESS";
    public static final String DISTANCE = "cz.tmapy.android.trex.DISTANCE";
    public static final String DURATION ="cz.tmapy.android.trex.DURATION";
    public static final String MAX_SPEED = "cz.tmapy.android.trex.MAX_SPEED";
    public static final String AVE_SPEED = "cz.tmapy.android.trex.AVE_SPEED";
    public static final String MIN_ALT = "cz.tmapy.android.trex.MIN_AlT";
    public static final String MAX_ALT = "cz.tmapy.android.trex.MAX_ALT";
    public static final String ELEV_DIFF_UP = "cz.tmapy.android.trex.ELEV_DIFF_UP";
    public static final String ELEV_DIFF_DOWN = "cz.tmapy.android.trex.ELEV_DIFF_DOWN";

}