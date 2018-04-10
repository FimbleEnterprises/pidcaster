package com.fimbleenterprises.torquebroadcaster;

import android.app.Application;
import android.content.Context;

/**
 * Created by Matt on 10/26/2016.
 */

public class MyApp extends Application {

    private static Context mContext;
    public static boolean TORQUE_IS_RUNNING = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }
}
