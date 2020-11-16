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
import android.os.RemoteException;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.fimbleenterprises.torquebroadcaster.utils.MyPID;

import org.prowl.torque.remote.ITorqueService;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
            }
        } catch (Exception e) {
            e.printStackTrace();

        }


        return super.onStartCommand(intent, flags, startId);
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

    public void start(Context context) {
        context.startService(new Intent(context, MyPidMonitorService.class));
        Log.i(TAG, "start: Starting the PID monitoring service!");

        if (torqueService == null) {
            bindToTorqueService();
        } else {
            startMonitoring();
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

    public void showNotification() {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showNotification("(" + pidsBeingMonitored.size() + ") Last broadcast: " +
                    new Date(System.currentTimeMillis()).toLocaleString());
        }*/
    }

    public void showNotification2() {

    }

    public void showNotification(String summaryText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (summaryText.equals("")) summaryText = "Broadcasts are being sent";
            String pluralizer = "PIDs";

            String pidList = "";
            for (int i = 0; i < pidsBeingMonitored.size(); i++) {
                try {
                    if (i <= 3) {
                        MyPID myPid = pidsBeingMonitored.get(i);
                        pidList += " ► " + myPid.fullName + "\n";
                    }
                } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
                    Log.e(TAG, "showNotification: " + indexOutOfBoundsException.getMessage());
                }
            }
            if (pidsBeingMonitored.size() > 3) {
                pidList += " ...and " + (pidsBeingMonitored.size() - 4) + " more";
            }

            if (pidList.contains("0")) {
                pidList = pidList.replace("...and 0 more", "");
            }

            int count = pidsBeingMonitored.size();
            if (count == 1) {
                pluralizer = "PID";
            } else {
                pluralizer = "PIDs";
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.broadcast_icon128x128);
            builder.setOngoing(true);
            builder.setWhen(System.currentTimeMillis());
            builder.setContentTitle("PID Broadcaster");
            builder.setTicker("PIDcaster started!");
            builder.setContentText("A Torque plugin");
            builder.setSmallIcon(R.drawable.broadcast_icon_tiny);
            builder.setLargeIcon(largeIcon);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            builder.setSubText(summaryText);
            NotificationCompat.BigTextStyle bigTextStyle =
                    new NotificationCompat.BigTextStyle()
                            .bigText(" Currently monitoring " + count + " " + pluralizer + ":\n" + pidList)
                            .setBigContentTitle("PID Broadcaster");
            builder.setStyle(bigTextStyle);

            Intent actionIntentGoTo = new Intent(this, NotificationReceiverActivity.class);
            actionIntentGoTo.setAction(NotificationReceiverActivity.INTENT_ACTION_GOTO_SERVICE);
            PendingIntent actionPendingIntentGoTo = PendingIntent.getActivity(this, 0, actionIntentGoTo,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(actionPendingIntentGoTo);

            Intent actionIntentQuit = new Intent(this, NotificationReceiverActivity.class);
            actionIntentQuit.setAction(NotificationReceiverActivity.INTENT_ACTION_STOP_SERVICE);
            PendingIntent actionPendingIntentQuit = PendingIntent.getActivity(this, 0, actionIntentQuit,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", actionPendingIntentQuit);

            Intent actionIntentStopTorque = new Intent(this, NotificationReceiverActivity.class);
            actionIntentQuit.setAction(INTENT_KILL_TORQUE);
            PendingIntent actionPendingIntentQuitTorque = PendingIntent.getActivity(this, 0, actionIntentStopTorque,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            // This kills Torque but fails to take this plugin with it - not worth addressing and
            // simply decided to omit this functionality.
            // builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "QUIT TORQUE", actionPendingIntentQuitTorque);

            // Sets an ID for the notification
            int mNotificationId = 001;
            // Gets an instance of the NotificationManager service
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = null;
            channel = new NotificationChannel("PID_CASTER_CHANNEL", "PID_CASTER", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("PID_CASTER");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = null;

            notificationManager = getSystemService(NotificationManager.class);

            notificationManager.createNotificationChannel(channel);
            // Builds the notification and issues it.
            mNotifyMgr.notify(mNotificationId, builder.build());
        }
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
                    showNotification();
                } catch (Exception e) {
                    Log.e(TAG, "startMonitoring: ");
                    e.printStackTrace();
                }
                myHandler.postDelayed(runner, options.getFrequency());
            }
        };
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
