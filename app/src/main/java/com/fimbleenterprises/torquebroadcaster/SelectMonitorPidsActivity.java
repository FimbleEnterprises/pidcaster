package com.fimbleenterprises.torquebroadcaster;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fimbleenterprises.torquebroadcaster.utils.MyPID;

import org.prowl.torque.remote.ITorqueService;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.NavUtils;

import static com.fimbleenterprises.torquebroadcaster.MyPidMonitorService.TAG;
import static com.fimbleenterprises.torquebroadcaster.MyPidMonitorService.allMyPIDs;
import static com.fimbleenterprises.torquebroadcaster.MyPidMonitorService.torqueService;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SelectMonitorPidsActivity extends ListActivity {

    MySettingsHelper options;
    SharedPreferences prefs;
    ListView listView;
    final Handler handler = new Handler();
    private static Runnable runner;
    private static List<MyPID> dynamicallyUpdatedPids;
    MyPidAdapter adapter;
    private static String[] supportedPids;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
/*            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);*/
        }
    };

    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };

    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_pid);

        this.options = new MySettingsHelper(getApplicationContext());
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mVisible = true;

        try {
            supportedPids = torqueService.listECUSupportedPIDs();
        } catch (Exception e) {
            Log.w(TAG, "FAILED TO GET ECU SUPPORTED PIDS!");
            e.printStackTrace();
        }

        // Set up the user interaction to manually show or hide the system UI.
/*        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });*/
        dynamicallyUpdatedPids = new ArrayList<>();
        buildListView();
    }

    @Override
    protected void onStop() {
        try {
            handler.removeCallbacks(runner);

        } catch (Exception e) {
            Log.d(TAG, "");

            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onStart() {
       try {
           if (runner == null) {
               runner = new Runnable() {
                   public void run() {
                       if (dynamicallyUpdatedPids == null) {
                           dynamicallyUpdatedPids = new ArrayList<>();
                       }
                       for (MyPID pid : dynamicallyUpdatedPids) {
                           pid.updatePIDValue(torqueService);
                       }
                       adapter.notifyDataSetChanged();
                       handler.postDelayed(this, 1000);
                   }
               };
           }
           handler.post(runner);
       } catch (Exception e) {
           Log.d(TAG, "FAILED TO START RUNNER");
           e.printStackTrace();
       }
        super.onStart();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
//        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to stopMonitoring the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void buildListView() {
        listView = getListView();

        if (MyPidMonitorService.allMyPIDs == null) {
            Toast.makeText(getApplicationContext(), "Service must be running", Toast.LENGTH_LONG).show();
            finish();
        } else {
            adapter = new MyPidAdapter(this, R.layout.list_row,
                    MyPidMonitorService.allMyPIDs);
            listView.setAdapter(adapter);

        }

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int firstVisibleRow = listView.getFirstVisiblePosition();
                int lastVisibleRow = listView.getLastVisiblePosition();
                dynamicallyUpdatedPids.clear();
                for(int i=firstVisibleRow;i<=lastVisibleRow;i++) {
                    System.out.println(i + "=" + listView.getItemAtPosition(i));
                    MyPID myPid = allMyPIDs.get(i);
                    if (!alreadyAdded(myPid.fullName)) {
                        Log.v(TAG, "onScroll: ADDED PID TO DYNAMIC UPDATE LIST:" + myPid.fullName);
                        dynamicallyUpdatedPids.add(myPid);
                    } else {
                        Log.i(TAG, "onScroll: " + myPid.fullName + " already being dynamically monitored");
                    }
                    // myPid.updatePIDValue(torqueService);
                }
            };
        });

        listView.setLongClickable(true);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final MyPID chosenPid = MyPidMonitorService.allMyPIDs.get(position);
                if ( ! chosenPid.isMonitored(options.getSharedPrefs())) {
                    chosenPid.operator = MyPID.AlarmOperator.SEND_ALWAYS;
                    chosenPid.threshold = 0f;
                    chosenPid.broadcastAction = new String(chosenPid.fullName + "_TORQUE").toLowerCase().replace(" ", "_").toLowerCase();
                    chosenPid.monitor(options.getSharedPrefs());
                    setResult(PreferencesActivity.RESULT_NEED_RESTART);
                    Toast.makeText(getApplicationContext(), "PID now monitored",
                            Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                } else {
                    chosenPid.stopMonitoring();
                    setResult(PreferencesActivity.RESULT_NEED_RESTART);
                    Toast.makeText(getApplicationContext(), "PID monitor removed.",
                            Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                }
                return true;
            }
        });
    }

    public boolean alreadyAdded(String fullName) {
        for (MyPID myPid : dynamicallyUpdatedPids) {
            if (fullName.equals(myPid.fullName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final MyPID chosenPid = MyPidMonitorService.allMyPIDs.get(position);
        final String[] operators = getResources().getStringArray(R.array.pid_monitor_operators);
        // custom dialogd
        final Dialog dialog = new Dialog(this);
        View layout = dialog.getLayoutInflater().inflate(R.layout.dialog_modify_pid, null);

        final TextView txtPidBeingMonitored = (TextView) layout.findViewById(R.id.textViewPidBeingEdited);
        txtPidBeingMonitored.setText(chosenPid.fullName);
        dialog.setContentView(layout);

        final TextView txtBroadcastAction = (TextView) layout.findViewById(R.id.textViewBroadcastAction);
        final EditText editTextBroadcastAction = (EditText) layout.findViewById(R.id.editText_broadcastAction);
        final TextView txtVarName = layout.findViewById(R.id.textView_varname);
        editTextBroadcastAction.setText(new String(chosenPid.fullName + "_TORQUE").toLowerCase().replace(" ", "_"));
        editTextBroadcastAction.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return false;
            }
        });
        final Spinner spinnerOperator = layout.findViewById(R.id.spinner_operator);
        spinnerOperator.requestFocus();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, operators);
        spinnerOperator.setAdapter(adapter);
        if (chosenPid.isMonitored(prefs)) {
            int itemInt;
            switch (chosenPid.operator) {
                case SEND_ALWAYS:
                    itemInt = 0;
                    break;
                case LESS_THAN:
                    itemInt = 1;
                    break;
                case GREATER_THAN:
                    itemInt = 2;
                    break;
                case EQUALS:
                    itemInt = 3;
                    break;
                case NOT_EQUALS:
                    itemInt = 4;
                    break;
                default:
                    itemInt = 0;
            }
            spinnerOperator.setSelection(itemInt);
        }
        spinnerOperator.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] operators = getResources().getStringArray(R.array.pid_monitor_operators);
                MyPID.AlarmOperator operator;
                float threshold = 0;
                switch (position) {
                    case 0:
                        operator = MyPID.AlarmOperator.SEND_ALWAYS;
                        break;
                    case 1:
                        operator = MyPID.AlarmOperator.LESS_THAN;
                        break;
                    case 2:
                        operator = MyPID.AlarmOperator.GREATER_THAN;
                        break;
                    case 3:
                        operator = MyPID.AlarmOperator.EQUALS;
                        break;
                    case 4:
                        operator = MyPID.AlarmOperator.NOT_EQUALS;
                        break;
                    default:
                        operator = MyPID.AlarmOperator.SEND_ALWAYS;
                }
                chosenPid.operator = operator;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        final EditText editTextThreshold = (EditText) layout.findViewById(R.id.editText_threshold);
        editTextThreshold.setSelectAllOnFocus(true);
        final Button btnCommit = (Button) layout.findViewById(R.id.btnCommitParams);
        if (chosenPid.isMonitored(prefs)) {
            editTextThreshold.setText(String.valueOf(chosenPid.threshold));
            editTextBroadcastAction.setText(new String(chosenPid.broadcastAction).replace(" ", "_"));
        } else {
            editTextThreshold.setText(String.valueOf(0));
        }

        dialog.setTitle("Choose Monitor Parameters");
        dialog.setCancelable(true);
        btnCommit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chosenPid.threshold = Float.parseFloat(editTextThreshold.getText().toString());
                chosenPid.broadcastAction = editTextBroadcastAction.getText().toString().toLowerCase();
                dialog.dismiss();
                chosenPid.monitor(options.getSharedPrefs());
                setResult(PreferencesActivity.RESULT_NEED_RESTART);
                Toast.makeText(getApplicationContext(), "PID now monitored",
                        Toast.LENGTH_SHORT).show();
            }
        });

        final Button btnRemoveMonitor = (Button) layout.findViewById(R.id.btnRemoveMonitor);
        btnRemoveMonitor.setEnabled(chosenPid.isMonitored(prefs));
        btnRemoveMonitor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chosenPid.stopMonitoring();
                setResult(PreferencesActivity.RESULT_NEED_RESTART);
                Toast.makeText(getApplicationContext(), "PID monitor removed.",
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    return true;
                } else {
                    return false;
                }
            }
        });
        txtVarName.setText("%" + chosenPid.broadcastAction);
        ImageButton btnClip = layout.findViewById(R.id.btnClipboard);
        btnClip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("pidcaster", "%" + chosenPid.broadcastAction);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();

    }

    public class MyPidAdapter extends ArrayAdapter {
        List<MyPID> data;
        TextView tvMain;
        TextView tvMinor;
        int layoutResourceId;
        Context context;
        SharedPreferences prefs;
        ITorqueService torqueService = MyPidMonitorService.torqueService;
        String[] mpids;
        String[] spids;
        private int dkRed = Color.argb(255,60,0,0);
        private int dkYellow = Color.argb(255,60,51,0);
        private int dkGreen = Color.argb(255, 0,96,0);
        private int dkGreenB = Color.argb(255, 0,50,0);


        public MyPidAdapter(Context context, int layoutResourceId, List<MyPID> data) {
            super(context, layoutResourceId, data);
            this.data = data;
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            try {
                mpids = torqueService.listActivePIDs();
                spids = torqueService.listECUSupportedPIDs();
            } catch (Exception e) {
                Log.w(TAG, "Failed to communicate with the Torque service.");
                e.printStackTrace();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View row = convertView;
            Holder holder = null;
            MyPID myPID = data.get(position);


            if(row == null) // this childView isn't cached, build it
            {
                LayoutInflater inflater = LayoutInflater.from(context);
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new Holder();

                // Find our radiobutton in the current childView
                TextView tvMain = (TextView)row.findViewById(R.id.textViewMainText);
                TextView tvMinor = (TextView)row.findViewById(R.id.textViewMinorText);
                ImageView imgIsMonitored = (ImageView)row.findViewById(R.id.imageViewIsMonitored);

                holder.tvMain = tvMain;
                holder.tvMinor = tvMinor;
                holder.imgViewIsMonitored = imgIsMonitored;

                row.setTag(holder);
            }
            else // this childView IS cached, reuse it yo
            {
                holder = (Holder) row.getTag();
            }

            // Get the text that will be assigned to the radiobutton
            holder.tvMain.setText(myPID.fullName);
            holder.tvMinor.setText("Last value: " + myPID.getRawValue());

            if (isEcuSupported(myPID.getRawPidID())) {
                row.setBackgroundColor(dkGreen);
            } else {
                row.setBackgroundColor(dkRed);
            }

            if (myPID.isMonitored(options.getSharedPrefs())) {
                holder.imgViewIsMonitored.setVisibility(View.VISIBLE);
            } else {
                holder.imgViewIsMonitored.setVisibility(View.INVISIBLE);
            }

            return row;
        }

        boolean isEcuSupported(String rawPidEntry) {
            boolean isSupported = false;
            for (String rawId : supportedPids) {
                isSupported = rawId.contains(rawPidEntry);
                if (isSupported) return true;
            }
            return false;
        }

        public class Holder
        {
            TextView tvMain;
            TextView tvMinor;
            ImageView imgViewIsMonitored;
        }
    }

}
