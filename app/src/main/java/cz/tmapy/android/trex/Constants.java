package cz.tmapy.android.trex;

/**
 * Created by kasvo on 4.8.2015.
 */
public final class Constants {

    //http://stackoverflow.com/questions/2018263/how-do-i-enable-disable-log-levels-in-android
    private static int LOGLEVEL = 3;
    public static boolean LOG_BASIC = LOGLEVEL > 1;
    public static boolean LOG_ENHANCED = LOGLEVEL > 2;

    // Intent extras key for determinig localization is running
    public static final String EXTRAS_LOCALIZATION_IS_RUNNING = "biz.svoboda.trex.LOCALIZATION_IS_RUNNING";

    // Defines a custom Intent action for position broadcasting
    public static final String LOCATION_BROADCAST = "biz.svoboda.trex.LOCATION_BROADCAST";
    public static final String EXTRAS_POSITION_DATA = "biz.svoboda.trex.EXTRAS_POSITION_DATA";
    public static final String EXTRAS_SERVER_RESPONSE = "biz.svoboda.trex.EXTRAS_SERVER_RESPONSE";
}