package com.fimbleenterprises.torquebroadcaster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import static com.fimbleenterprises.torquebroadcaster.MyPidMonitorService.INTENT_EXTRA_START_SERVICE;

/**
 * Created by Matt on 11/2/2016.
 */
public class MyTorqueIsStartingReceiver extends BroadcastReceiver {

    private static final String TAG = "QuittingReceiver";
    public static final String TORQUE_IS_STARTING = "org.prowl.torque.APP_LAUNCHED";
    public static final String TORQUE_IS_CONNECTED = "org.prowl.torque.OBD_CONNECTED";
    public static IntentFilter intentFilter = new IntentFilter(TORQUE_IS_STARTING);
    public static IntentFilter intentFilter2 = new IntentFilter(TORQUE_IS_CONNECTED);

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(TORQUE_IS_CONNECTED)) {
            Log.w(TAG, "onReceive: Received master Torque broadcast for CONNECTED");
        } else if (intent.getAction().equals(TORQUE_IS_STARTING)) {
            Log.w(TAG, "onReceive: Received master Torque broadcast for START");
        }

        Intent starter = new Intent(context, MyPidMonitorService.class);
        starter.putExtra(INTENT_EXTRA_START_SERVICE, true);
        context.startService(starter);
        Log.w(TAG, "onReceive: SENT START REQUEST FOR SERVICE!");
        MyApp.TORQUE_IS_RUNNING = true;
    }
}
