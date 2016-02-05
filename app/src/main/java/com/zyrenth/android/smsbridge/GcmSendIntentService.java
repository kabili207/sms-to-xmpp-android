package com.zyrenth.android.smsbridge;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.Random;

/**
 * Created by kabili on 1/28/16.
 */
public class GcmSendIntentService extends IntentService {

    public static final String ACTION = "action";

    public static final String ACTION_MESSAGE = "MESSAGE";
    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_DELIVERED = "DELIVERED";

    public static final String TIMESTAMP = "timestamp";
    public static final String MESSAGE = "message";
    public static final String MESSAGE_ID = "message_id";
    public static final String SENDER = "sender";
    public static final String CONTACT_NAME = "contact";

    public static final String REG_ID = "reg_id";
    public static final String PAIRING_CODE = "pairing_code";

    private static final String TAG = "GcmSendIntentService";

    static Random random = new Random();

    public GcmSendIntentService() {
        super("GcmSendIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();

        try {
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

            Bundle data = new Bundle();

            switch (extras.getString(ACTION)) {
                case ACTION_MESSAGE:
                    data.putString(ACTION, ACTION_MESSAGE);
                    data.putString(MESSAGE, extras.getString(MESSAGE));
                    data.putString(SENDER, extras.getString(SENDER));
                    data.putString(TIMESTAMP, extras.getString(TIMESTAMP));
                    if (extras.containsKey(CONTACT_NAME))
                        data.putString(CONTACT_NAME, extras.getString(CONTACT_NAME));
                    break;
                case ACTION_REGISTER:
                    data.putString(ACTION, ACTION_REGISTER);
                    data.putString(REG_ID, extras.getString(REG_ID));
                    if (extras.containsKey(PAIRING_CODE))
                        data.putString(PAIRING_CODE, extras.getString(PAIRING_CODE));
                    break;
                case ACTION_DELIVERED:
                    data.putString(ACTION, ACTION_DELIVERED);
                    data.putString(MESSAGE_ID, extras.getString(MESSAGE_ID));
                    data.putString(SENDER, extras.getString(SENDER));
                    break;
            }

            if (!data.isEmpty()) {
                String id = "m-" + Long.toString(random.nextLong());
                gcm.send(Utils.getSenderId(this) + "@gcm.googleapis.com", id, data);
            }

        } catch (IOException ex) {
            //msg = "Error :" + ex.getMessage();
            Log.e(TAG, "Invalid bundle", ex);
        }
    }
}
