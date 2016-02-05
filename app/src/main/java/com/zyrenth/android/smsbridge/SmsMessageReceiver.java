package com.zyrenth.android.smsbridge;

/**
 * Created by kabili on 1/28/16.
 */
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.telephony.SmsMessage;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SmsMessageReceiver extends BroadcastReceiver
{
    private static final String PDUS = "pdus";

    public void onReceive(Context context, Intent intent)
    {
        SharedPreferences preferences = Utils.getSharedPreferences(context);

        int permissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_SMS);

        if (preferences.getBoolean(Utils.PROPERTY_FORWARD_ENABLED, false) &&
                permissionCheck == PackageManager.PERMISSION_GRANTED)
        {
            Object[] pdus = (Object[]) intent.getExtras().get(PDUS);
            if (pdus != null && pdus.length > 0)
            {
                SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++)
                {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }

                SmsMessage message = messages[0];
                String text;
                if (messages.length == 1 || message.isReplace())
                {
                    text = message.getDisplayMessageBody();
                }
                else
                {
                    StringBuilder textBuilder = new StringBuilder();
                    for (SmsMessage msg : messages)
                    {
                        textBuilder.append(msg.getMessageBody());
                    }
                    text = textBuilder.toString();
                }


                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(message.getTimestampMillis());

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                sdf.setTimeZone(calendar.getTimeZone());
                String date = sdf.format(calendar.getTime());

                String number = message.getDisplayOriginatingAddress();

                String contactName = getContactDisplayNameByNumber(context, number);

                Intent i = new Intent(context, GcmSendIntentService.class);
                i.putExtra(GcmSendIntentService.ACTION, GcmSendIntentService.ACTION_MESSAGE);
                i.putExtra(GcmSendIntentService.TIMESTAMP, date);
                i.putExtra(GcmSendIntentService.MESSAGE, text);
                i.putExtra(GcmSendIntentService.SENDER, number);
                if(contactName != null) {
                    i.putExtra(GcmSendIntentService.CONTACT_NAME, contactName);
                }
                context.startService(i);
            }
        }
    }

    public String getContactDisplayNameByNumber(Context context, String number) {
        String name = null;

        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);

        if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

            ContentResolver contentResolver = context.getContentResolver();
            Cursor contactLookup = contentResolver.query(uri, new String[]{BaseColumns._ID,
                    ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

            try {
                if (contactLookup != null && contactLookup.getCount() > 0) {
                    contactLookup.moveToNext();
                    name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                    //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
                }
            } finally {
                if (contactLookup != null) {
                    contactLookup.close();
                }
            }
        }

        return name;
    }
}
