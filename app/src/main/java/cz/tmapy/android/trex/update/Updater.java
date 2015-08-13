package cz.tmapy.android.trex.update;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import cz.tmapy.android.trex.Constants;
import cz.tmapy.android.trex.R;

/**
 * Created by Kamil on 13. 8. 2015.
 */
public class Updater extends AsyncTask<Void, Void, String> {
    private static final String TAG = "Updater";

    private static String checkForUpdateUrl = "http://distrib.tmapy.cz/pub/distrib/t-rex/version.json";

    Context mContext;
    public Updater(Context context)
    {
        mContext = context;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            URL url = new URL(checkForUpdateUrl);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setDoOutput(true);
            c.connect();

            InputStream inputStream = c.getInputStream();

            BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"), 8);
            StringBuilder sBuilder = new StringBuilder();

            String line = null;
            while ((line = bReader.readLine()) != null) {
                sBuilder.append(line + "\n");
            }

            inputStream.close();
            return sBuilder.toString();

        } catch (Exception e) {
            if (Constants.LOG_BASIC) Log.e(TAG, "Check for update error!", e);
        }
        return null;
    }

    protected void onPostExecute(String versionJson) {
        if (versionJson != null && !versionJson.isEmpty() )
        {
            //parse JSON data
            try {
                int serverVersionCode = 0;
                JSONObject jObject = new JSONObject(versionJson);
                serverVersionCode = jObject.getInt("versionCode");

                PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                int versionCode = pInfo.versionCode;

                if (serverVersionCode > versionCode)
                    Toast.makeText(mContext, R.string.new_version_available, Toast.LENGTH_LONG).show();
                //else
                //    Toast.makeText(mContext, "Your version is up-to-date", Toast.LENGTH_SHORT).show();

            } catch (JSONException | PackageManager.NameNotFoundException e) {
                Toast.makeText(mContext, "Check application version error!", Toast.LENGTH_LONG).show();
                if (Constants.LOG_BASIC)  Log.e(TAG, "Check application version error ", e);
            }
        } else
        {
            Toast.makeText(mContext, "Cannot check new version!", Toast.LENGTH_LONG).show();
        }
    }
}
