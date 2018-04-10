package com.fimbleenterprises.torquebroadcaster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created by Matt on 10/19/2016.
 */

public class MyInternalReceiver extends BroadcastReceiver {

    public static final String TAG = "MyInternalReceiver";
    public static final String BROADCAST_INTENT_FILTER =
            "com.fimbleenterprises.torquebroadcaster.local_broadcast";
    public static final String BROADCAST_ACTION = "LOGGING_EVENT";

     public static IntentFilter intentFilter = new IntentFilter(BROADCAST_INTENT_FILTER);



    public static final String EXTRA_KEY_APPEND_LOG = "EXTRA_KEY_APPEND_LOG";

    public MyInternalReceiver() {
        Log.i(TAG, "MyInternalReceiver: Instantiated receiver");
        // intentFilter.addAction(BROADCAST_ACTION);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive: Received internal broadcast");
        /*Toast.makeText(context, "Received a broadcast!",
                Toast.LENGTH_SHORT).show();*/
    }
}
