package cz.tmapy.android.trex.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import cz.tmapy.android.trex.Const;
import cz.tmapy.android.trex.R;

/**
 * Created by Kamil on 13. 8. 2015.
 */
public class Updater extends AsyncTask<Void, Void, String> {
    private static final String TAG = "Updater";

    Activity mActivity;

    public Updater(Activity context) {
        mActivity = context;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            URL url = new URL(Const.CHECK_FOR_NEW_VER_URL);
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
            if (Const.LOG_BASIC) Log.e(TAG, "Check for update error!", e);
        }
        return null;
    }

    protected void onPostExecute(String versionJson) {
        if (versionJson != null && !versionJson.isEmpty()) {
            //parse JSON data
            try {
                int serverVersionCode = 0;
                JSONObject jObject = new JSONObject(versionJson);
                serverVersionCode = jObject.getInt("versionCode");

                PackageInfo pInfo = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0);
                int versionCode = pInfo.versionCode;

                if (serverVersionCode > versionCode) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(R.string.new_version_title)
                            .setMessage(R.string.new_version_message)
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Const.UPDATE_SITE_URL)));
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                } else
                    Toast.makeText(mActivity, R.string.app_up_to_date, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(mActivity, "Check application version error!", Toast.LENGTH_LONG).show();
                if (Const.LOG_BASIC) Log.e(TAG, "Check application version error ", e);
            }
        } else {
            Toast.makeText(mActivity, "Cannot check new version!", Toast.LENGTH_LONG).show();
        }
    }
}
