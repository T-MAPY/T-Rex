package cz.tmapy.android.trex;

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
    public static final String DIRECT_DISTANCE_TO_START = "cz.tmapy.android.trex.DIRECT_DISTANCE_TO_START";
    public static final String ESTIMATED_DISTANCE = "cz.tmapy.android.trex.ESTIMATED_DISTANCE";
    public static final String BEARING_TO_START = "cz.tmapy.android.trex.BEARING_TO_START";
    public static final String DURATION = "cz.tmapy.android.trex.DURATION";
    public static final String SERVER_RESPONSE = "cz.tmapy.android.trex.SERVER_RESPONSE";

}