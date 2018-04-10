package com.fimbleenterprises.torquebroadcaster;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class SettingsActivity extends AppCompatActivity {

    static Context myContext;
    public final String TAG = "SettingsActivity.";
    MySettingsHelper options;
    public static TextView txtEcuDisconnectedIntent;
    public static TextView txtEcuConnectedIntent;
    public static TextView txtPidZeroIntent;
    public static TextView txtPidNonZeroIntent;
    public static Button btnSaveIntentVals;
    public static Button btnSetDefaults;
    public static TextView txtSendValueIntent;
    public static ToggleButton btnAllowEditing;
    public static Button btnSetEngineMonitorPid;
    public static Button btnSetMonitoredPIDs;
    public static TextView txtFrequency;
    public static boolean isEditable;

    public static final String SAVE_VAL1 = "SAVE_VAL1";
    public static final String SAVE_VAL2 = "SAVE_VAL2";
    public static final String SAVE_VAL3 = "SAVE_VAL3";
    public static final String SAVE_VAL4 = "SAVE_VAL4";
    public static final String SAVE_VAL5 = "SAVE_VAL5";
    public static final String SAVE_VAL6 = "SAVE_VAL6";
    public static final String SAVE_VAL7 = "SAVE_VAL7";

    /** ON CREATE **/
    @SuppressLint("LongLogTag")
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        options = new MySettingsHelper(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        txtEcuDisconnectedIntent = (TextView) findViewById(R.id.EditText_OnEcuDisconnected);
        txtEcuDisconnectedIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSettingDialog(PID_BROADCAST_TO_CHANGE.ECU_DISCONNECTED);
            }
        });
        txtEcuConnectedIntent = (TextView) findViewById(R.id.EditText_OnEcuConnected);
        txtEcuConnectedIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSettingDialog(PID_BROADCAST_TO_CHANGE.ECU_CONNECTED);
            }
        });
        txtPidZeroIntent = (TextView) findViewById(R.id.editText_OnZeroValue);
        txtPidZeroIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSettingDialog(PID_BROADCAST_TO_CHANGE.ENGINE_RUNNING);
            }
        });
        txtPidNonZeroIntent = (TextView) findViewById(R.id.editText_OnNonZeroValue);
        txtPidNonZeroIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSettingDialog(PID_BROADCAST_TO_CHANGE.ENGINE_OFF);
            }
        });
        txtSendValueIntent = (TextView) findViewById(R.id.editTextSendValueIntent);
        btnSaveIntentVals = (Button) findViewById(R.id.btnSaveIntentValues);
        btnSetEngineMonitorPid = (Button) findViewById(R.id.btnEngineMonitorPid);

        btnSetMonitoredPIDs = (Button) findViewById(R.id.buttonMonitorList);
        btnSetMonitoredPIDs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SelectMonitorPidsActivity.class);
                startActivity(intent);
            }
        });
        btnAllowEditing = (ToggleButton) findViewById(R.id.toggleButtonFocusable);
        btnAllowEditing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isEditable = btnAllowEditing.isChecked();
                setTextViewsEditable();
            }
        });
        btnSaveIntentVals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePrefs();
            }
        });
        btnSetDefaults = (Button) findViewById(R.id.btnSetDefaults);
        btnSetDefaults.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDefaultPicker();
            }
        });
        txtFrequency = (TextView) findViewById(R.id.textView17);
        txtFrequency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSettingDialog(PID_BROADCAST_TO_CHANGE.FREQUENCY);
            }
        });

        if (savedInstanceState != null) {
            try {
                txtEcuConnectedIntent.setText(savedInstanceState.getString(SAVE_VAL1));
                txtEcuDisconnectedIntent.setText(savedInstanceState.getString(SAVE_VAL2));
                txtPidNonZeroIntent.setText(savedInstanceState.getString(SAVE_VAL3));
                txtPidZeroIntent.setText(savedInstanceState.getString(SAVE_VAL4));
                txtSendValueIntent.setText(savedInstanceState.getString(SAVE_VAL5));
                isEditable = savedInstanceState.getBoolean(SAVE_VAL6);
                txtFrequency.setText(savedInstanceState.getString(SAVE_VAL7));
                btnAllowEditing.setChecked(isEditable);
                setTextViewsEditable();
            } catch (Exception e) {
                Log.d(TAG, "Failed to reinstate the textViews from the savedInstanceState");

                e.printStackTrace();
            }
        } else {
            isEditable = false;
            btnAllowEditing.setChecked(isEditable);
            fillTextFields();
            setTextViewsEditable();

        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVE_VAL1, txtEcuConnectedIntent.getText().toString());
        outState.putString(SAVE_VAL2, txtEcuDisconnectedIntent.getText().toString());
        outState.putString(SAVE_VAL3, txtPidNonZeroIntent.getText().toString());
        outState.putString(SAVE_VAL4, txtPidZeroIntent.getText().toString());
        outState.putString(SAVE_VAL5, txtSendValueIntent.getText().toString());
        outState.putBoolean(SAVE_VAL6, btnAllowEditing.isChecked());
        outState.putString(SAVE_VAL7, txtFrequency.getText().toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        btnSetEngineMonitorPid.setText("ENGINE MONITOR PID: \n"+
                options.getEngineMonitorPid());


    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    public void savePrefs() {
        String ecuDisc = txtEcuDisconnectedIntent.getText().toString();
        String ecuConn = txtEcuConnectedIntent.getText().toString();
        String zeroVal = txtPidZeroIntent.getText().toString();
        String nonZero = txtPidNonZeroIntent.getText().toString();
        String pidVal = txtSendValueIntent.getText().toString();

        if (ecuDisc.length() > 0) options.setEcuDisconnectedIntent(ecuDisc);
        if (ecuConn.length() > 0) options.setEcuConnectedIntent(ecuConn);
        if (zeroVal.length() > 0) options.setOnZeroValueIntent(zeroVal);
        if (nonZero.length() > 0) options.setNonZeroValueIntent(nonZero);
        if (pidVal.length() > 0) options.setPidValueIntent(pidVal);
        try {
            String strFreq = txtFrequency.getText().toString();
            int freq = Integer.parseInt(strFreq);
            if (freq < 10) { freq = 10; }
            options.setFrequency(freq);
        } catch (Exception e) {
            Log.d(TAG, "FAILED TO SAVE PREF FREQUENCY!");
            e.printStackTrace();
        }

        fillTextFields();
        // setTextViewsFocusable(false);
        Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
    }

    public void showDefaultPicker() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Preferences")
                .setIcon(R.drawable.broadcast_icon128x128)
                .setMessage("Typically system-wide broadcasts are crafted to be unique since any " +
                        "installed app can see them.  That could make for problems if you make them so " +
                        "generic that your choice conflicts with another app's broadcast.  \n\n" +
                        "For example, if some crappy email app sent a broadcast titled, \"emailreceived\" " +
                        "and you have also (by some miracle) also chosen that name for one of these events " +
                        "then you might have a problem with when an email come in etc.")
                .setPositiveButton("Easy", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setEasyDefaults();
                        return;
                    }
                })
                .setNegativeButton("Safe", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setDefaults();
                        return;
                    }
                })

                .show();
    }


    public void fillTextFields() {
        txtEcuDisconnectedIntent.setText(options.getEcuDisconnectedIntent());
        txtEcuConnectedIntent.setText(options.getEcuConnectedIntent());
        txtPidNonZeroIntent.setText(options.getNonZeroValueIntent());
        txtPidZeroIntent.setText(options.getOnZeroValueIntent());
        txtSendValueIntent.setText(options.getPidValueIntent());
        txtFrequency.setText("" + options.getFrequency());

/*        setTextUnderlined(txtEcuConnectedIntent);
        setTextUnderlined(txtEcuDisconnectedIntent);
        setTextUnderlined(txtPidNonZeroIntent);
        setTextUnderlined(txtPidZeroIntent);
        setTextUnderlined(txtFrequency);*/
    }

    public void setTextViewsEditable() {
        setTextViewsEditable(isEditable);
    }

    public void setTextViewsEditable(boolean val) {
/*
        txtPidNonZeroIntent.setEnabled(val);
        txtPidZeroIntent.setEnabled(val);
        txtEcuConnectedIntent.setEnabled(val);
        txtEcuDisconnectedIntent.setEnabled(val);
        txtSendValueIntent.setEnabled(val);
        txtFrequency.setEnabled(val);

        txtPidNonZeroIntent.setFocusable(val);
        txtPidZeroIntent.setFocusable(val);
        txtEcuConnectedIntent.setFocusable(val);
        txtEcuDisconnectedIntent.setFocusable(val);
        txtSendValueIntent.setFocusable(val);
        txtFrequency.setFocusable(val);*/

    }

    public void setTextUnderlined(TextView tv) {
        SpannableString content = new SpannableString(tv.getText().toString());
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        tv.setText(content);
    }

    /*public void setTextViewsFocusable(boolean val) {
        txtPidNonZeroIntent.setEnabled(val);
        txtPidZeroIntent.setEnabled(val);
        txtEcuConnectedIntent.setEnabled(val);
        txtEcuDisconnectedIntent.setEnabled(val);
        txtSendValueIntent.setEnabled(val);
        txtFrequency.setEnabled(val);

        txtPidNonZeroIntent.setFocusable(val);
        txtPidZeroIntent.setFocusable(val);
        txtEcuConnectedIntent.setFocusable(val);
        txtEcuDisconnectedIntent.setFocusable(val);
        txtSendValueIntent.setFocusable(val);
        txtFrequency.setFocusable(val);

        txtPidNonZeroIntent.setEnabled(!val);
        txtPidZeroIntent.setEnabled(!val);
        txtEcuConnectedIntent.setEnabled(!val);
        txtEcuDisconnectedIntent.setEnabled(!val);
        txtSendValueIntent.setEnabled(!val);
        txtFrequency.setEnabled(!val);
    }*/

    public void changeSettingDialog(final PID_BROADCAST_TO_CHANGE whichOne) {
        final Dialog dialog = new Dialog(this);
        dialog.setTitle("Choose Monitor Parameters");
        dialog.setCancelable(true);
        View layout = dialog.getLayoutInflater().inflate(R.layout.choose_pid_monitor_parameters, null);
        dialog.setContentView(layout);
        final TextView txtViewOperatorLabel = (TextView) layout.findViewById(R.id.textView10);
        final TextView thresholdText = (TextView) layout.findViewById(R.id.textView11);
        txtViewOperatorLabel.setText("Frequency:");
        final TextView txtBroadcastAction = (TextView) layout.findViewById(R.id.textViewBroadcastAction);
        final EditText editTextBroadcastAction = (EditText) layout.findViewById(R.id.editText_broadcastAction);
        final TextView titleText = (TextView) layout.findViewById(R.id.textViewPidBeingEdited);

        thresholdText.setVisibility(View.GONE);
        final Spinner spinnerOperator = (Spinner) layout.findViewById(R.id.spinner_operator);
        spinnerOperator.setVisibility(View.GONE);
        final EditText editTextFrequency = (EditText) layout.findViewById(R.id.editText_threshold);
        editTextFrequency.setSelectAllOnFocus(true);

        switch (whichOne) {
            case ECU_CONNECTED:
                titleText.setText("While ECU Connected Broadcast");
                editTextBroadcastAction.setText(new String(options.getEcuConnectedIntent()).toLowerCase().replace(" ", "_"));
                break;
            case ECU_DISCONNECTED:
                titleText.setText("While ECU Disconnected Broadcast");
                editTextBroadcastAction.setText(new String(options.getEcuDisconnectedIntent()).toLowerCase().replace(" ", "_"));
                break;
            case ENGINE_OFF:
                titleText.setText("While Engine On Broadcast");
                editTextBroadcastAction.setText(new String(options.getNonZeroValueIntent()).toLowerCase().replace(" ", "_"));
                break;
            case ENGINE_RUNNING:
                titleText.setText("While Engine Off broadcast");
                editTextBroadcastAction.setText(new String(options.getOnZeroValueIntent()).toLowerCase().replace(" ", "_"));
                break;
            case FREQUENCY:
                titleText.setText("Set Broadcast Frequency");
                editTextFrequency.setText(""+options.getFrequency());
                break;
        }

        if (whichOne == PID_BROADCAST_TO_CHANGE.FREQUENCY) {
            txtBroadcastAction.setVisibility(View.GONE);
            editTextBroadcastAction.setVisibility(View.GONE);
            // thresholdText.setVisibility(View.VISIBLE);
            txtViewOperatorLabel.setVisibility(View.VISIBLE);
            editTextFrequency.setVisibility(View.VISIBLE);
        } else {
            txtBroadcastAction.setVisibility(View.VISIBLE);
            editTextBroadcastAction.setVisibility(View.VISIBLE);
            //thresholdText.setVisibility(View.GONE);
            txtViewOperatorLabel.setVisibility(View.GONE);
            editTextFrequency.setVisibility(View.GONE);
        }

        final Button btnCommit = (Button) layout.findViewById(R.id.btnCommitParams);
        btnCommit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switch (whichOne) {
                    case ECU_CONNECTED:
                        options.setEcuConnectedIntent(editTextBroadcastAction.getText().toString());
                        break;
                    case ECU_DISCONNECTED:
                        options.setEcuConnectedIntent(editTextBroadcastAction.getText().toString());
                        break;
                    case ENGINE_OFF:
                        options.setEcuConnectedIntent(editTextBroadcastAction.getText().toString());
                        break;
                    case ENGINE_RUNNING:
                        options.setEcuConnectedIntent(editTextBroadcastAction.getText().toString());
                        break;
                    case FREQUENCY:
                        options.setFrequency(Integer.parseInt(editTextFrequency.getText().toString()));
                        break;
                }

                Toast.makeText(getApplicationContext(), "Saved",
                        Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    enum PID_BROADCAST_TO_CHANGE {
        ECU_CONNECTED, ECU_DISCONNECTED, ENGINE_RUNNING, ENGINE_OFF, FREQUENCY
    }

    public void setBroadcastValue(PID_BROADCAST_TO_CHANGE whichOne, String newValue) {
        switch (whichOne) {
            case ECU_CONNECTED:
                options.setEcuConnectedIntent(newValue);
                break;
            case ECU_DISCONNECTED:
                options.setEcuDisconnectedIntent(newValue);
                break;
            case ENGINE_RUNNING:
                options.setNonZeroValueIntent(newValue);
                break;
            case ENGINE_OFF:
                options.setOnZeroValueIntent(newValue);
                break;
            case FREQUENCY:
                options.setFrequency(Integer.parseInt(newValue));
                break;
        }
    }

    public void setDefaults() {
        fillTextFields();
        options.clearAll();
        fillTextFields();
        savePrefs();
    }

    public void setEasyDefaults() {
        options.setEasyDefaults();
        fillTextFields();
        savePrefs();
    }



}