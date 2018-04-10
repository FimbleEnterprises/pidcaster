package com.fimbleenterprises.torquebroadcaster;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Matt on 11/3/2016.
 */

public class NotificationReceiverActivity extends Activity {

    public static final String TAG = "PLUGIN_RECEIVER";
    public static final String INTENT_ACTION_GOTO_SERVICE = "GOTO_SERVICE";
    public static final String INTENT_ACTION_STOP_SERVICE = "STOP_SERVICE";

    public static final String INTENT_EXTRA1 = "INTENT_EXTRA1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: Created Notification Receiver!");

        if (this.getIntent() != null) {
            Intent intent = getIntent();
            if (intent.getAction() != null) {
                if (intent.getAction().equals(INTENT_ACTION_GOTO_SERVICE)) {
                    Log.w(TAG, "onCreate: USER CLICKED THE CONFIG BUTTON IN NOTIFICATION!");
                    Intent newIntent = new Intent(this, PluginActivity.class);
                    startActivity(newIntent);
                    finish();
                } else if (intent.getAction().equals(INTENT_ACTION_STOP_SERVICE)) {
                    Log.w(TAG, "onCreate: USER CLICKED THE STOP SERVICE BUTTON IN NOTIFICATION!");
                    stopService(new Intent(this, MyPidMonitorService.class));
                    Intent quitAppIntent = new Intent(this, PluginActivity.class);
                    quitAppIntent.setAction(PluginActivity.INTENT_ACTION_QUIT);
                    startActivity(quitAppIntent);
                    finish();
                } else {
                    Log.i(TAG, "onCreate: NO INTENT PASSED!");
                    finish();
                }
            }
        }
    }
}
