package cz.tmapy.android.trex.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import cz.tmapy.android.trex.Const;
import cz.tmapy.android.trex.R;

/**
 * Broadcast receiver to start location service on system startup
 * Created by Kamil Svoboda on 15.3.2016.
 */
public class SystemStartupBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = SystemStartupBroadcastReceiver.class.getName();
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        if (mSharedPref.getBoolean(Const.PREF_KEY_STARTONSTARTUP, false)) {
            if (CheckProviders(context)) {
                Intent myIntent = new Intent(context, BackgroundLocationService.class);
                context.startService(myIntent);
            }
        }
    }

    /**
     * Check GPS status and airplane mode
     * @param context
     * @return
     */
    protected boolean CheckProviders(Context context)
    {
        boolean airplane_mode = true;
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            airplane_mode = android.provider.Settings.System.getInt(context.getContentResolver(),
                    android.provider.Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            airplane_mode = android.provider.Settings.Global.getInt(context.getContentResolver(),
                    android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }

        if (airplane_mode)
        {
            Toast.makeText(context, context.getResources().getString(R.string.cannot_start_localization_due_airplane), Toast.LENGTH_LONG).show();
            return false;
        }

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            Log.e(TAG, "Get GPS status exception", ex);
        }

        if (!gps_enabled) {
            Toast.makeText(context, context.getResources().getString(R.string.cannot_start_localization_due_gps_settings), Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
}