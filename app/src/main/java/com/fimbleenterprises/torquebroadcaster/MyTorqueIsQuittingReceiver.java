package com.fimbleenterprises.torquebroadcaster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created by Matt on 11/1/2016.
 */
public class MyTorqueIsQuittingReceiver extends BroadcastReceiver {

    private static final String TAG = "QuittingReceiver";
    public static final String TORQUE_IS_QUITTING = "org.prowl.torque.APP_QUITTING";
    public static IntentFilter intentFilter = new IntentFilter(TORQUE_IS_QUITTING);

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive: Received master Torque broadcast");
        MyPidMonitorService.quitPending = true;
        MyApp.TORQUE_IS_RUNNING = false;
        context.stopService(new Intent(context, MyPidMonitorService.class));
    }
}
