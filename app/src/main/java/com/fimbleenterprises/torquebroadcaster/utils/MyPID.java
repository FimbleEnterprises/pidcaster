package com.fimbleenterprises.torquebroadcaster.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fimbleenterprises.torquebroadcaster.MyApp;

import org.prowl.torque.remote.ITorqueService;

import static com.fimbleenterprises.torquebroadcaster.MyPidMonitorService.TAG;

/**
 * Created by Matt on 10/20/2016.
 */

public class MyPID {


    public enum ALARM_OPERATOR {
        LESS_THAN, GREATER_THAN, EQUALS, NOT_EQUALS, SEND_ALWAYS;
    }

    private static final String PREF_KEY_ALARM_ENTRY = "PREF_KEY_ALARM_ENTRY";
    public static final int RESET_VALUE_IF_NO_UPDATE_IN_SECONDS = 2;

    public String pid = null;
    public String fullName;
    public String shortName;
    public float min;
    public float max;
    public String unit;
    public float scale;
    public String equation;
    public float value;
    public String rawPIDEntry;
    private long lastUpdatedInMS;
    private static SharedPreferences prefs;
    private String isActive;

    /***************************************/
    /********* ALARM PROPERTIES ************/
    public float threshold;
    public ALARM_OPERATOR operator;
    private String strOperator;
    public String broadcastAction;
    /********* ALARM PROPERTIES ************/
    /***************************************/

    public void setValue(float value) { this.value = value;}

    /**
     * Returns the pid's most recent value
     * @return 0.0 if the data is stale (updated too long ago as specified by the user's timeout);
     */
    public float getValue() {
        if (getLastUpdatedInSeconds() > RESET_VALUE_IF_NO_UPDATE_IN_SECONDS) {
            return 0;
        }
        return this.value;
    }

    /**
     * Returns the pid's value
     * @return the pid's most recent value regardless of how stale the data is
     */
    public float getRawValue() {
        return this.value;
    }

    public MyPID() {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(MyApp.getContext());
    }

    public MyPID(String[] commaDelimitedPidInfo, float[] commaDelimitedPidValues) {
        String[] pidInfo = commaDelimitedPidInfo[0].split(",");
        this.fullName = pidInfo[0];
        this.shortName = pidInfo[1];
        this.unit = pidInfo[2];
        this.max = Float.parseFloat(pidInfo[3]);
        this.min = Float.parseFloat(pidInfo[4]);
        this.scale = Float.parseFloat(pidInfo[5]);
        this.value = commaDelimitedPidValues[0];

    }

    public void setPrefs(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public long getLastUpdatedInMS() {
        return (System.currentTimeMillis() - lastUpdatedInMS);
    }

    public long getLastUpdatedInSeconds() {
        return (this.getLastUpdatedInMS() / 1000);
    }

    public String[] getRawValueForTorqueServiceQueries() {
        return new String[]{this.rawPIDEntry};
    }

    public String getRawPidID() {
        String[] strings = rawPIDEntry.split(",");
        return strings[0];
    }

    /**
     * Returns this PID's current raw value as well as updating its raw value
     * @param torqueService
     * @return
     */
    public float updatePIDValue(ITorqueService torqueService) {
        try {

            float[] vals = torqueService.getPIDValues(this.getRawValueForTorqueServiceQueries());
            this.value = vals[0];
            return this.value;
        } catch (Exception e) {
            Log.e(TAG, "updatePIDValue: Updated values");
            e.printStackTrace();
        } finally {

        }
        return this.value;
    }

    public void setlastUpdatedInMS(long lastUpdatedInMS) {
        this.lastUpdatedInMS = lastUpdatedInMS;
    }

    private static String interpretOperator(ALARM_OPERATOR operator)  {
        switch (operator) {
            case LESS_THAN:
                return "<";
            case GREATER_THAN:
                return ">";
            case EQUALS:
                return "==";
            case NOT_EQUALS:
                return "!=";
            case SEND_ALWAYS:
                return "--";
        }
        return "";
    }

    private static ALARM_OPERATOR interpretOperator(String operator) {
        if (operator == null) return null;
        else if (operator.equals("<")) return ALARM_OPERATOR.LESS_THAN;
        else if (operator.equals(">")) return ALARM_OPERATOR.GREATER_THAN;
        else if (operator.equals("==")) return ALARM_OPERATOR.EQUALS;
        else if (operator.equals("!=")) return ALARM_OPERATOR.NOT_EQUALS;
        else if (operator.equals("--")) return ALARM_OPERATOR.SEND_ALWAYS;
        return null;
    }

    public void monitor(SharedPreferences prefs) {
        String fullString = "";
        fullString = this.fullName + "|" + interpretOperator(operator) + "|" + String.valueOf(this.threshold) + "|" +
                broadcastAction + "|" + this.isActive + "|" + this.getRawPidID();
        prefs.edit().putString(PREF_KEY_ALARM_ENTRY + "|" + this.fullName, fullString).apply();
    }

    /**
            * Updates the PID's value by querying the ECU for a current value.  By default values are metric.
            * <br/><br/>
            * If the user's preferences suggest that the PID values ought to be converted to imperial this
            * function tries to convert the current PID's value from metric.  It should be successful if the
            * PID's UNIT property is determined as either meters, kilometers or celsius.  If the UNIT value
            * is of another type (K/PA etc.) or if a known UNIT type's conversion fails then the fallback
            * value will always be updated as metric.
    * @param pidRawValue The PID's .getRawValue() value
    * @param unitType The PID's .unitType() value
    * @return The supplied value, as a Float converted to imperial.
            */
    public static float tryConvertToImperial(float pidRawValue, String unitType) {
        if (pidRawValue == 0) {
            return 0;
        } else if (unitType.contains("km")) {
            double meters = (pidRawValue * 1000);
            double feet = (meters * 3.280839895);
            double miles = (feet / 5280);
            return (float) miles;
        } else if (unitType.equals("m")) {
            float meters = pidRawValue * 3.280839895f;
            return (meters);
        } else if (unitType.equals("°C")) {
            float farenheit = (pidRawValue * 1.8f) + 32f;
            return farenheit;
        } else {
            return pidRawValue;
        }
    }

    /**
     * Removes the selected PidMonitor from SharedPreferences.
     * <br/><br/>
     * <b>NOTE:</b>
     * This returns no confirmation of success.  If confirmation is desired you can call the
     * overloaded version of this method passing a boolean <i>true</i> parameter
     * <i>(stopMonitoring(true))</i>.  Verifying deletion does incur additional overhead.
     */
    public void stopMonitoring() {
        prefs.edit().remove(PREF_KEY_ALARM_ENTRY + "|" + this.fullName).apply();
    }




    /**
     * Evaluates the supplied value against the previously specified alarm threshold
     * using the operator stipulated at the alarm's creation.
     * @param valueToEvaluate Current float value of the pid to evaluate.
     * @return true if the threshold is breached and false if not.
     */
    public boolean shouldBroadcast(float valueToEvaluate) {
        switch (this.operator) {
            case EQUALS:
                return (valueToEvaluate == threshold);
            case NOT_EQUALS:
                return (valueToEvaluate != threshold);
            case GREATER_THAN:
                return (valueToEvaluate > threshold);
            case LESS_THAN:
                return (valueToEvaluate < threshold);
            case SEND_ALWAYS:
                return true;
            default:
                return true;
        }
    }



    private void setMonitorParams(String fullEntry) {
        String[] array = fullEntry.split("\\|");
        this.fullName = array[0];
        this.operator = interpretOperator(array[1]);
        this.threshold = Float.parseFloat(array[2]);
        this.broadcastAction = array[3];
        this.isActive = array[4];
        this.rawPIDEntry = array[5];
    }

    public boolean isMonitored(SharedPreferences prefs) {
        String fullString = prefs.getString(PREF_KEY_ALARM_ENTRY + "|" + fullName, null);
        if (fullString != null) {
            setMonitorParams(fullString);
            return true;
        } else {
            return false;
        }
    }

    private void isActive(boolean active) {
        if (active) {
            this.isActive = "true";
        } else {
            this.isActive = "false";
        }
    }

}
