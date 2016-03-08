package cz.tmapy.android.trex.service;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import cz.tmapy.android.trex.MainScreen;

/**
 * Created by Kamil Svoboda on 8.3.2016.
 */
public class ServiceHelper {
    private static final String TAG = ServiceHelper.class.getName();
    /**
     * Check for google play services availability
     *
     * @return
     */
    public static boolean checkPlayServices(Activity activity) {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int errorCheck = api.isGooglePlayServicesAvailable(activity);
        if (errorCheck == ConnectionResult.SUCCESS) {
            //google play services available, hooray
            return true;
        } else if (api.isUserResolvableError(errorCheck)) {
            //GPS_REQUEST_CODE = 1000, and is used in onActivityResult
            api.showErrorDialogFragment(activity, errorCheck, 1111);
            //stop our activity initialization code
        }
        Log.e(TAG, "Google Play Services not available");
        Toast.makeText(activity, "Google Play Services not available", Toast.LENGTH_LONG).show();
        return false;
    }

    /**
     * Check service is running
     *
     * @param serviceClass
     * @return
     */
    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
