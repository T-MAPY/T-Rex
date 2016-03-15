package cz.tmapy.android.trex.security;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper class for communication security
 * Created by Kamil Svoboda on 15.3.2016.
 */
public class SecurityHelper {
    private static final String TAG = SecurityHelper.class.getName();

    /**
     * Generate MD5 hash from string
     *
     * @param inputString
     * @return
     */
    public static byte[] GetMd5Hash(String inputString) {

        try {
            byte[] bytesOfMessage = inputString.getBytes("UTF-8");
            MessageDigest md = null;
            md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(bytesOfMessage);
            return thedigest;

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Can't get MD5 hash", e);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Can't get MD5 hash", e);
        }

        return null;
    }

    /**
     * Returns MD5 hash based on security string, device identifier and location time
     *
     * @param deviceId
     * @param dateTime
     * @param mSecurityString
     * @return
     */
    public static String GetMd5ForLocation(String deviceId, Date dateTime, String mSecurityString) {
        Format format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String dateString = format.format(dateTime);
        //Log.d(TAG,"Input string: " + deviceId + dateString + mSecurityString);

        byte[] md5 = GetMd5Hash(deviceId + dateString + mSecurityString);
        String base64 = Base64.encodeToString(md5, Base64.NO_WRAP);
        //Log.d(TAG, "Result security string: " + base64);
        return base64;
    }
}
