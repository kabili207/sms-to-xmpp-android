package com.zyrenth.android.smsbridge;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int MY_PERMISSIONS_REQUEST_READ_SMS = 7;

    @InjectView(R.id.send)
    Button send;

    @InjectView(R.id.saveProjectId)
    Button saveProjectId;

    @InjectView(R.id.saveForwarning)
    Button saveForwarning;

    @InjectView(R.id.display)
    TextView display;

    @InjectView(R.id.senderIdInputView)
    EditText senderId;

    @InjectView(R.id.forwardingEnabled)
    SwitchCompat forwardingEnabled;

    @InjectView(R.id.senderIdPanel)
    View senderIdPanel;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.registrationProgressBar)
    ProgressBar mRegistrationProgressBar;

    BroadcastReceiver mRegistrationBroadcastReceiver;
    GoogleCloudMessaging gcm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        setSupportActionBar(toolbar);


        send.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                /*Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, "Your Android SMS Gateway Registration ID");
                intent.putExtra(Intent.EXTRA_TEXT, Utils.getRegistrationId(MainActivity.this));
                startActivity(Intent.createChooser(intent, "Send Email"));
                */

                Intent i = new Intent(MainActivity.this, GcmSendIntentService.class);
                i.putExtra(GcmSendIntentService.ACTION, GcmSendIntentService.ACTION_REGISTER);
                i.putExtra(GcmSendIntentService.REG_ID, Utils.getRegistrationId(MainActivity.this));

                String pairingCode = Utils.getPairingCode(MainActivity.this);
                if (!TextUtils.isEmpty(pairingCode)) {
                    i.putExtra(GcmSendIntentService.PAIRING_CODE, pairingCode);
                }
                startService(i);
            }
        });
        saveProjectId.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                senderIdPanel.setEnabled(false);
                mRegistrationProgressBar.setVisibility(ProgressBar.VISIBLE);

                Utils.setSenderId(MainActivity.this, senderId.getText().toString());

                // Start IntentService to register this application with GCM.
                Intent intent = new Intent(MainActivity.this, RegistrationIntentService.class);
                startService(intent);
            }
        });

        // Don't show this until we need it
        mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                send.setVisibility(View.VISIBLE);
                senderIdPanel.setVisibility(View.GONE);

                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(Utils.PROPERTY_SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                   // mInformationTextView.setText(getString(R.string.gcm_send_message));
                } else {
                  //  mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };

        forwardingEnabled.setChecked(Utils.isForwardingEnabled(this));
        forwardingEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    requestSmsRead();
                }
            }
        });

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices())
        {
            gcm = GoogleCloudMessaging.getInstance(this);
            if (TextUtils.isEmpty(Utils.getRegistrationId(this)))
            {
                send.setVisibility(View.INVISIBLE);
                display.setText("");
            }
            else
                senderIdPanel.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Utils.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean checkPlayServices()
    {
        return GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
    }

    private void requestSmsRead() {

        String[] desiredPerms = new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS
        };

        List<String> requestingPerms = new ArrayList<>();

        for (int i = 0; i < desiredPerms.length; i++) {
            if (ContextCompat.checkSelfPermission(this, desiredPerms[i]) != PackageManager.PERMISSION_GRANTED) {
                requestingPerms.add(desiredPerms[i]);
            }
        }

        if (requestingPerms.size() == 0) {
            Utils.setForwardingEnabled(MainActivity.this, true);
        } else {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_SMS)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        requestingPerms.toArray(new String[requestingPerms.size()]),
                        MY_PERMISSIONS_REQUEST_READ_SMS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_SMS: {
                // If request is cancelled, the result arrays are empty.
                for(int i = 0; i < permissions.length; i++) {
                    if(permissions[i] == Manifest.permission.READ_SMS) {
                        boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                        if (!granted) {
                            forwardingEnabled.setChecked(false);
                        }
                        Utils.setForwardingEnabled(MainActivity.this, granted);
                    }
                }

                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}
