package com.zyrenth.android.smsbridge;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmListenerService;

import java.util.ArrayList;

/**
 * Created by kabili on 1/31/16.
 */
public class MyGcmListenerService extends GcmListenerService {
    private static final String TAG = "MyGcmListenerService";

    private static final String ACTION_SEND_SMS = "send_sms";
    private static final String ACTION_PAIR = "pair";
    private static final String ACTION_PAIR_SUCCESS = "pair_success";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {

        Log.d(TAG, "From: " + from);

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else if (data.containsKey("action")){
            String action = data.getString("action");

            switch (action) {
                case ACTION_SEND_SMS:
                    sendSms(data);
                    break;
                case ACTION_PAIR:
                    showPairInstruction(data);
                    break;
                case ACTION_PAIR_SUCCESS:
                    showPairedNotification(data);
                    break;
            }
        } else {
            Log.e(TAG, "GCM Message received with no action");
        }
    }

    private void sendSms(Bundle data) {

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);

        if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
            final String number = data.getString("number");
            final String message = data.getString("message");
            final String message_id = data.getString("message_id");

            if (!TextUtils.isEmpty(number) && !TextUtils.isEmpty(message)) {

                try {
                    String SENT = "SMS_SENT";
                    String DELIVERED = "SMS_DELIVERED";

                    PendingIntent sentPI = null;
                    PendingIntent deliveredPI = null;
/*
                    if (!TextUtils.isEmpty(message_id)) {

                        sentPI = PendingIntent.getBroadcast(this, 0,
                                new Intent(SENT), 0);

                        //---when the SMS
                        deliveredPI = PendingIntent.getBroadcast(this, 0,
                                new Intent(DELIVERED), 0);

                        //---when the SMS has been sent---
                        registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context arg0, Intent arg1) {
                                switch (getResultCode()) {
                                    case Activity.RESULT_OK:
                                       // Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                                unregisterReceiver(this);
                            }
                        }, new IntentFilter(SENT));

                        //---when the SMS has been delivered---
                        registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context arg0, Intent arg1) {
                                switch (getResultCode()) {
                                    case Activity.RESULT_OK:
                                        sendDeliveryReport(number, message_id);
                                        break;
                                }
                                unregisterReceiver(this);
                            }
                        }, new IntentFilter(DELIVERED));

                    }
*/
                    SmsManager smsManager = SmsManager.getDefault();
                    ArrayList<String> msgArray = smsManager.divideMessage(message);

                    if(msgArray.size() > 1) {
                        smsManager.sendMultipartTextMessage(number, null, msgArray, null, null);
                    } else {
                        smsManager.sendTextMessage(number, null, message, null, null);
                    }

                    String result = number + ": " + message;
                    Log.i(TAG, result);
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
            }
        }
    }

    private void sendDeliveryReport(String sender, String messageId) {
        Intent i = new Intent(this, GcmSendIntentService.class);
        i.putExtra(GcmSendIntentService.ACTION, GcmSendIntentService.ACTION_DELIVERED);
        i.putExtra(GcmSendIntentService.MESSAGE_ID, messageId);
        startService(i);
    }

    private void showPairInstruction(Bundle data) {
        String pairingCode = data.getString("pairing_code");
        String jid = data.getString("server_jid");

        sendNotification("Pairing required",
                String.format("Send %s to %s", pairingCode, jid),
                String.format("Please send the message %s to the address %s to complete pairing.", pairingCode, jid));
    }

    private void showPairedNotification(Bundle data) {
        String jid = data.getString("paired_jid");

        sendNotification("Pairing successful",
                String.format("Linked to account %s", jid),
                String.format("Your phone is now linked to the account %s.", jid));
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String title, String message) {
        sendNotification(title, message, null);
    }

    private void sendNotification(String title, String shortMessage, String largeMessage) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle(title)
                .setContentText(shortMessage)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);
        if(largeMessage != null) {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(largeMessage));
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
