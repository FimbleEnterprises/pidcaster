package com.fimbleenterprises.torquebroadcaster;


import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import static com.fimbleenterprises.torquebroadcaster.PluginActivity.TAG;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class PreferencesActivity extends PreferenceActivity {

    public static final int RESULT_OKAY = 0;
    public static final int RESULT_NOT_OKAY = 1;
    public static final int RESULT_NEED_RESTART = 2;
    public static Context context;
    public static MySettingsHelper options;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new MyPreferenceFragment()).commit();
        setupActionBar();
        options = new MySettingsHelper(this);
        context = getApplicationContext();
    }




    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();


            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            }  else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };


    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


    public static class MyPreferenceFragment extends PreferenceFragment  {
        Preference prefChoosePids;
        private static final String TAG = "MyPrefereceFragment";

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            Log.i(TAG, "onCreate: OnCreate");

            prefChoosePids = findPreference(getString(R.string.PREF_CHOOSE_PIDS));
            prefChoosePids.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), SelectMonitorPidsActivity.class);
                    startActivityForResult(intent, 0);
                    return false;
                }
            });

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            view.setBackgroundColor(getResources().getColor(android.R.color.black));

            return view;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            Log.i(TAG, "onActivityResult: RETURNED WITH RESULT!");
            if (requestCode == 0) {
                switch (resultCode) {
                    case RESULT_NEED_RESTART :
                        restartService();
                        break;
                    default:
                        return;
                }
            }
        }
    }

    private static void restartService() {
        Log.i(TAG, "onActivityResult: RETURNED!");
        MyApp.getContext().stopService(new Intent(MyApp.getContext(), MyPidMonitorService.class));
        Toast.makeText(MyApp.getContext(), "Restarting broadcaster...",
                Toast.LENGTH_SHORT).show();
        final Handler h = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                Intent intent = new Intent(context, MyPidMonitorService.class);
                intent.putExtra(MyPidMonitorService.INTENT_EXTRA_START_SERVICE,true);
                context.startService(intent);
            }
        };
        h.postDelayed(r, 1500);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setIcon(R.drawable.broadcast_icon128x128);
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void changeSettingDialog(final SettingsActivity.PID_BROADCAST_TO_CHANGE whichOne) {
        final Dialog dialog = new Dialog(this);
        dialog.setTitle("Choose Monitor Parameters");
        dialog.setCancelable(true);
        View layout = dialog.getLayoutInflater().inflate(R.layout.choose_pid_monitor_parameters, null);
        dialog.setContentView(layout);



    }


/*

    */

    /*    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }*/


}
