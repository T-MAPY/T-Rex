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
    protected static final String UTF8 = "UTF-8";
    /**
     * Generate MD5 hash from string
     *
     * @param inputString
     * @return
     */
    public static byte[] GetMd5Hash(String inputString) {

        try {
            byte[] bytesOfMessage = inputString.getBytes(UTF8);
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
     * Returns MD5 hash based on access key, device identifier and location time
     *
     * @param deviceId
     * @param dateTime
     * @param mAccessKey
     * @return
     */
    public static String GetSecurityString(String deviceId, Date dateTime, String mAccessKey) {
        Format format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String dateString = format.format(dateTime);
        //Log.d(TAG,"Input string: " + deviceId + dateString + mAccessKey);

        byte[] md5 = GetMd5Hash(deviceId + dateString + mAccessKey);
        String base64 = GetBase64String(md5);
        //Log.d(TAG, "Result security string: " + base64);
        return base64;
    }

    public static String GetBase64String(byte[] input) {
        return Base64.encodeToString(input, Base64.NO_WRAP);
    }

    public static String GetBase64FromUTF8String(String inputString) {
        try {
            return Base64.encodeToString(inputString.getBytes(UTF8), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupported encoding", e);
        }
        return null;
    }
}
