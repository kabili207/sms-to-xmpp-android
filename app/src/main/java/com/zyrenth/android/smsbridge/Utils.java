package com.zyrenth.android.smsbridge;

import android.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

/**
 * Created by kabili on 1/31/16.
 */
public class Utils {

    static final String TAG = "Utils";

    public static final String REGISTRATION_COMPLETE = "registration_complete";

    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_SENDER_ID = "sender_id";
    public static final String PROPERTY_FORWARD_ENABLED = "forwarding_enabled";
    public static final String PROPERTY_SENT_TOKEN_TO_SERVER = "sent_token";

    public static final String PROPERTY_APP_VERSION = "app_version";
    public static final String PROPERTY_PAIRING_CODE = "pairing_code";

    public static SharedPreferences getSharedPreferences(Context context){
        return context.getSharedPreferences(context.getApplicationContext().getPackageName(), Context.MODE_PRIVATE);
    }

    public static boolean canSendSms(Context context) {
        return ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isForwardingEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(PROPERTY_FORWARD_ENABLED, false);
    }

    public static void setForwardingEnabled(Context context, boolean enabled) {
        getSharedPreferences(context)
                .edit()
                .putBoolean(PROPERTY_FORWARD_ENABLED, enabled)
                .apply();
    }

    public static void setPairingCode(Context context, String pairingCode) {
        getSharedPreferences(context)
                .edit()
                .putString(PROPERTY_PAIRING_CODE, pairingCode)
                .apply();
    }

    public static String getPairingCode(Context context)
    {
        return getSharedPreferences(context).getString(PROPERTY_PAIRING_CODE, "");
    }

    public static void setRegistrationId(Context context, String regId){
        getSharedPreferences(context)
                .edit()
                .putString(PROPERTY_REG_ID, regId)
                .putInt(PROPERTY_APP_VERSION, getAppVersion(context))
                .apply();
    }

    public static String getRegistrationId(Context context)
    {
        SharedPreferences prefs = getSharedPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.length() == 0)
        {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        if (registeredVersion != getAppVersion(context))
        {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    public static String getSenderId(Context context)
    {
        return getSharedPreferences(context).getString(PROPERTY_SENDER_ID, "");
    }

    public static void setSenderId(Context context, String senderId)
    {
        getSharedPreferences(context)
                .edit()
                .putString(PROPERTY_SENDER_ID, senderId)
                .apply();
    }

    public static int getAppVersion(Context context)
    {
        try
        {
            return context.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(context.getApplicationContext().getPackageName(), 0)
                    .versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

}
