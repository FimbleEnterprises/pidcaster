package com.fimbleenterprises.torquebroadcaster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.fimbleenterprises.torquebroadcaster.utils.Helpers;
import com.fimbleenterprises.torquebroadcaster.utils.MyPID;

import org.joda.time.DateTime;
import org.prowl.torque.remote.ITorqueService;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;
import static com.fimbleenterprises.torquebroadcaster.MyInternalReceiver.EXTRA_KEY_APPEND_LOG;

public class MyPidMonitorService extends Service {
    public static final String TAG = "MainActivity";
    public static final String TORQUE_PACKAGE_NAME = "org.prowl.torque";
    public static final String TORQUE_CLASS_NAME = "org.prowl.torque.remote.TorqueService";
    public static final String SERVICE_IS_ON_TEXT = "PID Broadcaster: Sending broadcasts";
    public static final String SERVICE_IS_OFF_TEXT = "PID Broadcaster: Not broadcasting";
    public static final String ECU_CONNECTED = "Connected to ECU: Yup";
    public static final String ECU_NOT_CONNECTED = "Connected to ECU: Nope";
    public static final String BUTTON_START_STOP_STARTED = "Stop broadcasting";
    public static final String BUTTON_START_STOP_STOPPED = "Start broadcasting";
    public static boolean isPaused = false;
    public static boolean quitPending = false;

    public static ITorqueService torqueService;
    public static boolean isBound = false;
    private NumberFormat nf;
    MyHandler myHandler = new MyHandler();
    Runnable runner;
    MySettingsHelper options;
    public static List<MyPID> allMyPIDs;
    SharedPreferences prefs;
    public static IBinder binder;
    public static final String INTENT_EXTRA_START_SERVICE = "INTENT_EXTRA_START_SERVICE";
    public static final String INTENT_EXTRA_STOP_SERVICE = "INTENT_EXTRA_STOP_SERVICE";
    public static final String LOCAL_BROADCAST = "com.fimbleenterprises.torquebroadcaster.UPDATE_ACTIVITY";
    public static final String INTENT_KILL_TORQUE = "org.prowl.torque.REQUEST_TORQUE_QUIT";

    public static boolean connectedToECU = false;
    public static boolean serviceRunning = false;
    // public static MyPID engineMonitorPid;
    private static boolean runnerRunning;
    public static String[] allPIDs;
    public List<MyPID> pidsBeingMonitored;
    public static String textToLog = "";
    NotificationManager mNotificationManager;
    Notification notification;
    public static final int NOTIFICATION_ID = 111;
    public static final String NOTIFICATION_CHANNEL = "PIDcaster_NOTIFICATION_CHANNEL";
    public static final String NOTIFICATION_ACTION = "NOTICATION_ACTION";
    public static final int REQUEST_CODE = 3;
    public static final String WAKELOCK_ACQUIRED = "WAKELOCK_AQUIRED";
    public static final String WAKELOCK_RELEASED = "WAKELOCK_RELEASED";
    PowerManager.WakeLock wakeLock;
    public static String killReason = "Service was killed";

    PendingIntent pIntent;

    public MyPidMonitorService() {
        // bindToTorqueService();
        Log.i(TAG, "MyPidMonitorService: Instantiated!");

    }

    @Override
    public IBinder onBind(Intent intent) {
        // throw new UnsupportedOperationException("Not yet implemented");
        Log.e(TAG, "onBind");
        isBound = true;
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind");
        isBound = false;
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.e(TAG, "onRebind");
        isBound = true;
        super.onRebind(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        options = new MySettingsHelper(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            Bundle extras = intent.getExtras();
            Log.e(TAG, "onStartCommand: " + extras.size());
            Log.e(TAG, "isBound: " + isBound);
        } catch (Exception e) {
            Log.e(TAG, "NO EXTRAS@");
            
            e.printStackTrace();
        }
        try {
            if (PluginActivity.btnStartStopService != null) {
                PluginActivity.btnStartStopService.setEnabled(false);
            }
            boolean startServiceRequested = intent.getBooleanExtra(INTENT_EXTRA_START_SERVICE, false);
            if (startServiceRequested) {
                quitPending = false;
                nf = NumberFormat.getNumberInstance();
                bindToTorqueService();
                Toast.makeText(this, "Starting PIDcaster", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

        startInForeground();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

/*        if (torqueService == null) {
            bindToTorqueService();
        } else {
            startMonitoring();
        }*/

    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: Stopped service!");
        // Intent intent = new Intent();
        // intent.putExtra(MyPidMonitorService.INTENT_EXTRA_STOP_SERVICE, true);
        // getApplicationContext().sendBroadcast(intent);
        // PluginActivity.txtLog.append("Sent broadcast: " + MyPidMonitorService.INTENT_EXTRA_STOP_SERVICE + "\n");

        try {
            serviceRunning = false;
            mNotificationManager.cancelAll();
            myHandler.removeCallbacks(runner);
            Log.e(TAG, "onDestroy:isBound=" + isBound);
            if (isBound) {
                unbindService(connection);
            }
            if (PluginActivity.btnStartStopService != null) {
                PluginActivity.btnStartStopService.setEnabled(true);
                PluginActivity.btnStartStopService.setText("Start Service");
                // PluginActivity.btnStartStopService.setBackgroundResource(R.drawable.btn_start_broadcasting);

            }
            releaseWakelock();
            Toast.makeText(this, "Stopping PIDcaster", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: ");
            e.printStackTrace();
        } finally {
            hideNotification();
            if (PluginActivity.btnStartStopService != null) {
                if (MyPidMonitorService.serviceRunning) {
                    PluginActivity.txtServiceStatus.setTextColor(Color.parseColor("#BABABA"));
                    PluginActivity.txtServiceStatus.setText(SERVICE_IS_ON_TEXT);
                    PluginActivity.btnStartStopService.setText(BUTTON_START_STOP_STARTED);
                    // PluginActivity.btnStartStopService.setBackgroundResource(R.drawable.btn_stop_broadcasting);
                } else {
                    PluginActivity.txtServiceStatus.setTextColor(Color.RED);
                    PluginActivity.txtServiceStatus.setText(SERVICE_IS_OFF_TEXT);
                    PluginActivity.btnStartStopService.setText(BUTTON_START_STOP_STOPPED);
                    // PluginActivity.btnStartStopService.setBackgroundResource(R.drawable.btn_start_broadcasting);
                }
            }
        }
        super.onDestroy();
    }

    public void stop() {
        try {
            if (isBound) {
                unbindService(connection);
            }
            myHandler.removeCallbacks(runner);
            this.stopSelf();
        } catch (Exception e) {
            Log.e(TAG, "stopService: ");
            stopSelf();
            e.printStackTrace();
        } finally {

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void start(Context context) {
        context.startService(new Intent(context, MyPidMonitorService.class));
        Log.i(TAG, "start: Starting the PID monitoring service!");
        startInForeground();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void startInForeground() {
        Log.i(TAG, "startInForeground ");
        getWakelock();
        if (torqueService == null) {
            bindToTorqueService();
        } else {
            startMonitoring();
        }
        getNotification("PIDcaster", "PIDcaster is running");
        startForeground(NOTIFICATION_ID, getNotification("PIDcaster", "PIDcaster is running"));
    }

    private void getWakelock() {
        Log.d(TAG, "getWakelock Acquiring wakelock...");
        PowerManager mgr = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "PIDcaster:MyWakeLock");
        wakeLock.acquire();
        Log.d(TAG, "getWakelock Wakelock is held = " + wakeLock.isHeld());

        if (wakeLock.isHeld()) {

        } else {
            if (! wakeLock.isHeld()) {
                Log.i(TAG, "getWakelock NOT HELD!");
                wakeLock.acquire();
            }
        }
    }

    private void releaseWakelock() {
        if (wakeLock == null) return;

        Log.d(TAG, "releaseWakelock Wakelock is held = " + wakeLock.isHeld());

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public static void startService() {
        if (! MyPidMonitorService.quitPending) {
            Intent intent = new Intent(MyApp.getContext(), MyPidMonitorService.class);
            intent.putExtra(MyPidMonitorService.INTENT_EXTRA_START_SERVICE,true);
            MyApp.getContext().startService(intent);
        }
    }

    public void bindToTorqueService() {
        try {
            // Bind to the torque service
            boolean successfulBind = false;
            Intent intent = new Intent();
            intent.setClassName(TORQUE_PACKAGE_NAME, TORQUE_CLASS_NAME);
            try {
                Log.e(TAG, "bindToTorqueService:isBound=" + isBound);
                successfulBind = bindService(intent, connection, 0);
            } catch (Exception e) {
                Log.e(TAG, "bindToTorqueService: ");
                e.printStackTrace();
            } finally {
                // null
            }
            if (successfulBind) {
                Log.i(TAG, "onResume: BOUND TO OBDLINK");
                // startMonitoring();

                // Not really anything to do here.
                // Once you have bound to the service, you can start calling
                // methods on torqueService.someMethod()  - look at the aidl
                // file for more info on the calls
            }
        } catch (Exception e) {
            Log.e(TAG, "bindToTorqueService: FAILED TO BIND SERVICE, YO!");
            e.printStackTrace();
        } finally {
            Log.i(TAG, "bindToTorqueService: Is our service running?  Answer: " +
                    MyPidMonitorService.serviceRunning);
        }
    }

    /**
     * Bits of service code. You usually won't need to change this.
     */
    public ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.e(TAG, "-= onServiceConnected =-");
            torqueService = ITorqueService.Stub.asInterface(service);
            binder = service;
            isBound = true;
            try {
                if (torqueService.getVersion() < 19) {
                    Toast.makeText(getApplicationContext(), "You are using an " +
                            "old version of Torque with this plugin.\n\nThe plugin needs the latest" +
                            " version of Torque to run correctly.\n\nPlease upgrade to the latest version" +
                            " of Torque from Google Play", Toast.LENGTH_LONG).show();
                    return;
                }
                // torqueService.setDebugTestMode(true);
                startMonitoring();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        };

        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, " -= onServiceDisconnected =-");
            isBound = false;
            torqueService = null;
        };
    };

    public boolean checkConnectivity() {
        if (torqueService == null) return false;
        try {
            return torqueService.isConnectedToECU();
        } catch (Exception e) {
            Log.e(TAG, "checkConnectivity: Failed to check connectivity!");
            e.printStackTrace();
        } finally {

        }
        return false;
    }

    public static ITorqueService getTorqueService() {
        return torqueService;
    }

    public void sendGlobalIntents() {
        StringBuilder toBeLogged = new StringBuilder("<br/>");

        Intent intent;

        String now = new Date(System.currentTimeMillis()).toLocaleString();
        String dashLine = "-------------------------";

        toBeLogged.append("<p>" + now + "<br/>" + dashLine + "<br/>");

        if (connectedToECU) {
            intent = new Intent(options.getEcuConnectedIntent());
            getApplicationContext().sendBroadcast(intent);
            toBeLogged.append("•&nbsp Sent default broadcast with action: <font color=\"#008499\"> " +
                    intent.getAction() + "</font><br/><br/>");
        } else {
            intent = new Intent(options.getEcuDisconnectedIntent());
            getApplicationContext().sendBroadcast(intent);
            toBeLogged.append("•&nbsp Sent default broadcast with action: <font color=\"#008499\"> " +
                    intent.getAction() + "</font><br/><br/>");
        }

        if ( quitPending) {
            Log.e(TAG, "sendGlobalIntents: QUIT PENDING!");
            return;
        }

        int index = 1;

        for (MyPID pid : pidsBeingMonitored) {
            if (pid != null) {

                // Update the PIDs value as of this moment.
                pid.updatePIDValue(torqueService);

                // Check that the user's preferences for this pid dictate that it should broadcast its value
                if (pid.shouldBroadcast(pid.getRawValue())) {

                    boolean useMetric = options.getUseMetric();
                    float valToBroadcast = 0;
                    if (useMetric) {
                        valToBroadcast = pid.getRawValue();
                    } else {
                        valToBroadcast = MyPID.tryConvertToImperial(pid.getRawValue(), pid.unit);
                    }

                    // Construct a broadcast intent, assign an extra containing the pids value
                    intent = new Intent(pid.broadcastAction);
                    intent.putExtra(pid.broadcastAction, valToBroadcast);
                    
                    // Send the system-wide broadcast
                    getApplicationContext().sendBroadcast(intent);

                    // Construct a logentry for the PluginActivity
                    String alarmString = "•&nbsp BROADCAST " + index +
                            "<br/> &nbsp &nbsp &nbsp INTENT ACTION:  <font color=\"#008499\"> " + intent.getAction() + "</font>" +
                            "<br/> &nbsp &nbsp &nbsp INTENT EXTRA: <font color=\"#008499\">" + intent.getAction() + "</font>" +
                            "<br/> &nbsp &nbsp &nbsp Payload: (float): <font color=\"#008499\">" + valToBroadcast + "</font><br/>" +
                            "<br/> &nbsp &nbsp &nbsp <font color=\"#FFFFFF\">TASKER USAGE:</font>" +
                            "<br/> &nbsp &nbsp &nbsp Variable value name:<font color=\"#FFFFFF\">" + "%" + intent.getAction() + "</font>" +
                            "<br/> &nbsp &nbsp &nbsp Variable value will be:<font color=\"#FFFFFF\">" + valToBroadcast + "</font><br/><br/>";
                    toBeLogged.append(alarmString);
                    Log.v(TAG, "sendGlobalIntents: ALARM:<br/>" + alarmString);
                    index++;
                } // end shouldBroadcast
            } // end (pid != null)
        } // end (MyPID pid : pidsBeingMonitore))

        toBeLogged.append(dashLine + "</p>");

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Connected to ECU: " + connectedToECU + "\n");
        for (MyPID pid : pidsBeingMonitored) {
            stringBuilder.append("PID monitor: " + pid.fullName + ": " + pid.value + "\n");
        }
        stringBuilder.append("Last updated: " + Helpers.DatesAndTimes.getPrettyDateAndTime(DateTime.now()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateNotification(stringBuilder.toString());
        }
        // Send a local broadcast to the PluginActivity with the log entry
        sendLocalIntents(toBeLogged.toString());

    }

    private void sendLocalIntents(String textToAppend) {
        try {
            // Construct a local broadcast intent
            Intent localIntent = new Intent();
            localIntent.setAction(MyInternalReceiver.BROADCAST_INTENT_FILTER);
            // Add the last log entry values as an extra
            localIntent.putExtra(EXTRA_KEY_APPEND_LOG, textToAppend);
            // Send the broadcast
            sendBroadcast(localIntent);
        } catch (Exception e) {
            Log.d(TAG, "");
            stopSelf();
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void updateNotification(String text) {

        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getApplicationContext()
                    .getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
        }

        // Notification notification = getNotification("PIDcaster is running", text);
        mNotificationManager.notify(NOTIFICATION_ID, getNotification("PIDcaster is running", text));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    Notification getNotification(String title, String contentText) {
        Notification.Builder mBuilder =
                new Notification.Builder(this.getApplicationContext());

        Intent i = new Intent(this.getApplicationContext(), PluginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Intent ii = new Intent(this.getApplicationContext(), PluginActivity.class);
        ii.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ii.setAction(NOTIFICATION_ACTION);

        PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), REQUEST_CODE, i
                , PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent stopTripIntent = PendingIntent.getActivity(this.getApplicationContext(), REQUEST_CODE
                , ii, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setOngoing(true);
        mBuilder.setSmallIcon(R.mipmap.app_icon);
        mBuilder.setContentTitle(title);
        mBuilder.setDefaults(Notification.DEFAULT_ALL);
        mBuilder.setContentText(contentText);
        mBuilder.setStyle(new Notification.BigTextStyle()
                .bigText(contentText));
        mBuilder.addAction(R.drawable.btn_stop_broadcasting, getString(R.string.stop_trip_btn_notification_text),
                stopTripIntent);


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setSmallIcon(R.mipmap.app_icon);
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.app_icon));
            mBuilder.setColor(Color.WHITE);
        } else {
            Log.i(TAG, "getNotification ");
        }

        mNotificationManager = (NotificationManager) getApplicationContext()
                .getSystemService(getApplicationContext().NOTIFICATION_SERVICE);

        mNotificationManager.cancelAll();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = NOTIFICATION_CHANNEL;
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    NOTIFICATION_CHANNEL,
                    IMPORTANCE_LOW);
            channel.setVibrationPattern(new long[]{ 0 });
            channel.enableVibration(true);
            channel.enableLights(false);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channel.getId());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel.enableVibration(false);
            } else {
                mBuilder.setVibrate(new long[]{0L});
            }

        }

        notification = mBuilder.build();
        return notification;
    }

    public void hideNotification() {
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancelAll();
    }

    List<MyPID> getAllMyPids() {
        List<MyPID> myPids = new ArrayList<>();
        Log.i(TAG, "getAllMyPids: Building...");
        if (quitPending) {
            return null;
        }
        try {
            if (allPIDs == null || allPIDs.length < 1) {
                allPIDs = torqueService.listAllPIDs();
            }

            allMyPIDs = new ArrayList<>();
            for (String pidEntry: allPIDs) {

                // Get the data from the ECU
                MyPID myPID = new MyPID(torqueService.getPIDInformation(new String[]{pidEntry}),
                        torqueService.getPIDValues(new String[]{pidEntry}));
                myPID.rawPIDEntry = pidEntry;
                myPID.setPrefs(getApplicationContext());

                // Add it to the arraylist
                myPids.add(myPID);
            }
        } catch (Exception e) {
            Log.d(TAG, "FAILED TO GET MYPIDS");
            e.printStackTrace();
        }
        return myPids;
    }

    public void startMonitoring() {

        pidsBeingMonitored = new ArrayList<>();
        allMyPIDs = getAllMyPids();

        if (allMyPIDs == null) {
            return;
        }

        for (MyPID myPID :
                allMyPIDs) {
            if (myPID.isMonitored(options.getSharedPrefs())) {
                pidsBeingMonitored.add(myPID);
            }
        }

        // engineMonitorPid = findPID(options.getEngineMonitorPid(), false);
        // startMonitoring();
        serviceRunning = true;
        if (PluginActivity.btnStartStopService != null) {
            PluginActivity.btnStartStopService.setText("Stop Service");
        }

        if (allMyPIDs == null || allMyPIDs.size() < 1) {
            allMyPIDs = getAllMyPids();
        }

        if (runner != null) {
            try {
                myHandler.removeCallbacks(runner);
                myHandler.removeCallbacksAndMessages(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        runner = new Runnable() {
            @Override
            public void run() {
                if (PluginActivity.btnStartStopService != null) {
                    PluginActivity.btnStartStopService.setEnabled(true);
                    PluginActivity.btnStartStopService.setText(BUTTON_START_STOP_STARTED);
                    // PluginActivity.btnStartStopService.setBackgroundResource(R.drawable.btn_stop_broadcasting);
                    PluginActivity.txtServiceStatus.setText(SERVICE_IS_ON_TEXT);
                }

                if ( ! options.serviceAllowedToRun()) {
                    myHandler.removeCallbacks(runner);
                    stopSelf();
                }

                if (quitPending) {
                    myHandler.removeCallbacks(runner);
                    stopSelf();
                    return;
                }

                try {
                    connectedToECU = checkConnectivity();
                    // Send intents
                    sendGlobalIntents();
                    //sendLocalIntents();

                } catch (Exception e) {
                    Log.e(TAG, "startMonitoring: ");
                    e.printStackTrace();
                }
                myHandler.postDelayed(runner, options.getFrequency());
            }
        };


        StringBuilder stringBuilder = new StringBuilder();
        for (MyPID pid : pidsBeingMonitored) {
            stringBuilder.append("Connected to ECU: " + connectedToECU + "\n");
            stringBuilder.append(pid.fullName + ": " + pid.value + "\n");
        }

        stringBuilder.append("Last updated: " + Helpers.DatesAndTimes.getPrettyDateAndTime(DateTime.now()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateNotification(stringBuilder.toString());
        }

        Log.i(TAG, "startMonitoring: Starting the service!");
        myHandler.post(runner);
    }

    public static class MyHandler extends Handler {
        public MyHandler() {
            super();
        }

        public MyHandler(Callback callback) {
            super(callback);
        }

        public MyHandler(Looper looper) {
            super(looper);
        }

        public MyHandler(Looper looper, Callback callback) {
            super(looper, callback);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }

        @Override
        public void dispatchMessage(Message msg) {
            Log.i(TAG, "dispatchMessage:" + msg.arg1);
            super.dispatchMessage(msg);
        }

        @Override
        public String getMessageName(Message message) {
            Log.i(TAG, "getMessageName: " + message.arg1);
            return super.getMessageName(message);
        }

        @Override
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            Log.i(TAG, "sendMessageAtTime: " + msg.arg1);
            return super.sendMessageAtTime(msg, uptimeMillis);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

 /*   private void startMonitoring() {
        pidsBeingMonitored = new ArrayList<>();
        allMyPIDs = getAllMyPids();
        for (MyPID myPID :
                allMyPIDs) {
            if (myPID.isMonitored(options.getSharedPrefs())) {
                pidsBeingMonitored.add(myPID);
            }
        }

        // engineMonitorPid = findPID(options.getEngineMonitorPid(), false);
        // startMonitoring();
        serviceRunning = true;
        PluginActivity.btnStartStopService.setText("Stop Service");

        if (allMyPIDs == null || allMyPIDs.size() < 1) {
            allMyPIDs = getAllMyPids();
        }
    }*/


    /*    *//**
     * Do an update of PIDs we know about (that the settings_activity app has sent us)
     *//*
    private MyPID findPID(String startsWith, boolean useRawValue)  {

        Log.i(TAG, "findPID: " + startsWith);
        if (torqueService == null) {return null;}

        try {

            if (allMyPIDs == null || allMyPIDs.size() < 1) {
                Log.w(TAG, "findPID: allMyPIDs was null or had no elements.  Will rebuild");
                allMyPIDs = getAllMyPids();
            }

            if (useRawValue) {
                // Try to find the desired PID by fullname
                for (MyPID myPID: allMyPIDs) {
                    if (myPID.rawPIDEntry.startsWith(startsWith)) {
                        return myPID;
                    }
                }
            } else {
                // Try to find the desired PID by fullname
                for (MyPID myPID: allMyPIDs) {
                    if (myPID.fullName.startsWith(startsWith)) {
                        return myPID;
                    }
                }
            }

        } catch(Exception e) {
            Log.e(getClass().getCanonicalName(),e.getMessage(),e);
        }
        return null;
    }*/
}
