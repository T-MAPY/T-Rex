package cz.tmapy.android.trex.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import cz.tmapy.android.trex.Const;

/**
 * Broadcast receiver to start location service on system startup
 * Created by Kamil Svoboda on 15.3.2016.
 */
public class SystemStartupBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        if (mSharedPref.getBoolean(Const.PREF_STARTONSTARTUP, false)) {
            Intent myIntent = new Intent(context, BackgroundLocationService.class);
            context.startService(myIntent);
        }
    }
}