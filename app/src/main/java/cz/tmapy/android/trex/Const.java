package cz.tmapy.android.trex;

/**
 * Created by kasvo on 4.8.2015.
 */
public final class Const {

    //http://stackoverflow.com/questions/2018263/how-do-i-enable-disable-log-levels-in-android
    private static int LOGLEVEL = 3;
    public static boolean LOG_BASIC = LOGLEVEL > 1;
    public static boolean LOG_ENHANCED = LOGLEVEL > 2;

    //Preferences keys
    public static final String PREF_KEY_DEVICE_ID = "pref_id";
    public static final String PREF_KEY_TARGET_SERVUER_URL = "pref_targetUrl";
    public static final String PREF_KEY_KEEP_SCREEN_ON = "pref_screen_on";
    public static final String PREF_STRATEGY = "pref_strategy";
    public static final String PREF_FREQUENCY = "pref_frequency";
    public static final String PREF_MIN_DIST = "pref_min_dist";
    public static final String PREF_MAX_TIME= "pref_max_time";

    // Intent extras key for determinig localization is running
    public static final String EXTRAS_LOCALIZATION_IS_RUNNING = "biz.svoboda.trex.LOCALIZATION_IS_RUNNING";

    // Defines a custom Intent action for position broadcasting
    public static final String LOCATION_BROADCAST = "biz.svoboda.trex.LOCATION_BROADCAST";
    public static final String EXTRAS_POSITION_DATA = "biz.svoboda.trex.EXTRAS_POSITION_DATA";
    public static final String EXTRAS_SERVER_RESPONSE = "biz.svoboda.trex.EXTRAS_SERVER_RESPONSE";
}