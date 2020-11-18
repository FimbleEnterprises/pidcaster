package com.fimbleenterprises.torquebroadcaster;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.fimbleenterprises.torquebroadcaster.utils.MyPID;

/**
 * Created by Matt on 10/19/2016.
 */

public class MySettingsHelper {

    // DEFAULT INTENT NAMES
    public static final String DEFAULT_INTENT_NON_ZERO = "com.fimbleenterprises.torquebroadcast.NONZERO";
    public static final String DEFAULT_INTENT_ZERO = "com.fimbleenterprises.torquebroadcast.ZERO";
    public static final String DEFAULT_INTENT_ECU_DISCONNECTED = "com.fimbleenterprises.torquebroadcast.ECU_DISCONNECTED";
    public static final String DEFAULT_INTENT_ECU_CONNECTED = "com.fimbleenterprises.torquebroadcast.ECU_CONNECTED";
    public static final String DEFAULT_INTENT_SENDVALUE = "com.fimbleenterprises.torquebroadcast.PID_VALUE";

    public static final String PREF_KEY_PID_VALUE_INTENT = "PREF_KEY_PID_VALUE_INTENT";
    public static final String PREF_KEY_SERVICE_CAN_RUN = "PREF_KEY_SERVICE_CAN_RUN";
    public static final String PREF_KEY_ZERO_INTENT_VALUE = "PREF_KEY_ZERO_INTENT_VALUE";
    public static final String PREF_KEY_NONZERO_INTENT_VALUE = "PREF_KEY_NONZERO_INTENT_VALUE";
    public static final String PREF_KEY_ECU_DISCONNECTED_INTENT_VALUE = "PREF_KEY_ECU_DISCONNECTED_INTENT_VALUE";
    public static final String PREF_KEY_ECU_CONNECTED_INTENT_VALUE = "PREF_KEY_ECU_CONNECTED_INTENT_VALUE";
    public static final String PREF_KEY_MONITOR_PID = "PREF_KEY_MONITOR_PID";
    public static final String PREF_KEY_FREQUENCY = "PREF_KEY_FREQUENCY";
    public static final String PREF_KEY_USE_METRIC = "PREF_KEY_USE_METRIC";
    public static final String SELECT_PID_MONITORS = "PREF_KEY_FREQUENCY";
    public static final String PREF_KILL_WITH_TORQUE = "PREF_KILL_WITH_TORQUE";

    private Context context;
    private SharedPreferences prefs;
    public MySettingsHelper(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    public void clearAll() {
        prefs.edit().clear().apply();
        prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    public void setEngineMonitorPid(String longName) {
        prefs.edit().putString(PREF_KEY_MONITOR_PID, longName).apply();
    }

    public void setEngineMonitorPid(MyPID pid) {
        setEngineMonitorPid(pid.fullName);
    }

    public void setFrequency(int frequency) {
        prefs.edit().putString(PREF_KEY_FREQUENCY, String.valueOf(frequency)).apply();
    }

    public int getFrequency() {
        String strFreq = prefs.getString(PREF_KEY_FREQUENCY, "2000");
        return Integer.parseInt(strFreq);
    }

    /**
     * Gets the long name of the PID to monitor - the service can find it based on this name
     * @return
     */
    public String getEngineMonitorPid() {
        return prefs.getString(PREF_KEY_MONITOR_PID, "Engine Load");
    }

    public void setEasyDefaults() {
        setPidValueIntent("PID_VALUE_TORQUE");
        setNonZeroValueIntent("DATA_TORQUE");
        setOnZeroValueIntent("NO_DATA_TORQUE");
        setEcuConnectedIntent("YES_ECU_TORQUE");
        setEcuDisconnectedIntent("NO_ECU_TORQUE");
    }

    public void setSafeDefaults() {
        setPidValueIntent("com.fimbleenterprises.torquebroadcaster.PID_VALUE_TORQUE");
        setNonZeroValueIntent("com.fimbleenterprises.torquebroadcaster.DATA_TORQUE");
        setOnZeroValueIntent("com.fimbleenterprises.torquebroadcaster.NO_DATA_TORQUE");
        setEcuConnectedIntent("com.fimbleenterprises.torquebroadcaster.YES_ECU_TORQUE");
        setEcuDisconnectedIntent("com.fimbleenterprises.torquebroadcaster.NO_ECU_TORQUE");
    }

    public void serviceAllowedToRun(boolean value) {
        prefs.edit().putBoolean(PREF_KEY_SERVICE_CAN_RUN, value).apply();
    }

    public boolean killWithTorque() {
        return prefs.getBoolean(PREF_KILL_WITH_TORQUE, true);
    }

    public void killWithTorque(boolean val) {
        prefs.edit().putBoolean(PREF_KILL_WITH_TORQUE, val).commit();
    }

    public boolean serviceAllowedToRun() {
        return prefs.getBoolean(PREF_KEY_SERVICE_CAN_RUN, true);
    }

    public void setOnZeroValueIntent(String value) {
        prefs.edit().putString(PREF_KEY_ZERO_INTENT_VALUE, value).apply();
    }

    public String getOnZeroValueIntent() {
        return prefs.getString(PREF_KEY_ZERO_INTENT_VALUE, DEFAULT_INTENT_ZERO);
    }

    public void setNonZeroValueIntent(String value) {
        prefs.edit().putString(PREF_KEY_NONZERO_INTENT_VALUE, value).apply();
    }

    public String getNonZeroValueIntent() {
        return prefs.getString(PREF_KEY_NONZERO_INTENT_VALUE, DEFAULT_INTENT_NON_ZERO);
    }

    public void setEcuDisconnectedIntent(String value) {
        prefs.edit().putString(PREF_KEY_ECU_DISCONNECTED_INTENT_VALUE, value).apply();
    }

    public String getEcuDisconnectedIntent() {
        return prefs.getString(PREF_KEY_ECU_DISCONNECTED_INTENT_VALUE, DEFAULT_INTENT_ECU_DISCONNECTED);
    }

    public void setEcuConnectedIntent(String value) {
        prefs.edit().putString(PREF_KEY_ECU_CONNECTED_INTENT_VALUE, value).apply();
    }

    public String getEcuConnectedIntent() {
        return prefs.getString(PREF_KEY_ECU_CONNECTED_INTENT_VALUE, DEFAULT_INTENT_ECU_CONNECTED);
    }

    public void setPidValueIntent(String value) {
        prefs.edit().putString(PREF_KEY_PID_VALUE_INTENT, value).apply();
    }

    public void setUseMetric(boolean useMetric) {
        prefs.edit().putBoolean(PREF_KEY_USE_METRIC, useMetric).apply();
    }

    public boolean getUseMetric() {
        return prefs.getBoolean(PREF_KEY_USE_METRIC, true);
    }

    public String getPidValueIntent() {
        return prefs.getString(PREF_KEY_PID_VALUE_INTENT, DEFAULT_INTENT_SENDVALUE);
    }
    
    public SharedPreferences getSharedPrefs() {
        return prefs;
    }

    public void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    /**
     * Gets a single String preference
     * @param key The key value for the saved preference
     * @return The string preference specified by the key.
     * <br/>
     * <br/>
     * NOTE: returns a zero-length string if the preference isn't found.
     */
    public String getString(String key) {
        return prefs.getString(key, "");
    }

    /**
     * Gets a single String preference
     * @param key The key value for the saved preference
     * @param defaultValue The default value to return if the preference isn't found
     * @return The string preference specified by the key
     */
    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }



}
