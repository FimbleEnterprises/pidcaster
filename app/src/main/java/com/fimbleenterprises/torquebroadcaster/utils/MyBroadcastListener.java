package com.fimbleenterprises.torquebroadcaster.utils;

import android.content.Context;
import android.content.Intent;

/**
 * Created by Matt on 10/27/2016.
 */

public interface MyBroadcastListener {
        void onMyBroadcastReceived(Context context, Intent intent);
}
