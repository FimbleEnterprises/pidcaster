package com.fimbleenterprises.torquebroadcaster;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.fimbleenterprises.torquebroadcaster.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import static android.R.id;


public class PluginActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {

    public static final String TAG = "MainActivity";
    private static final int PREFERENCES_REQUEST_CODE = 0x01;
    public MySettingsHelper options;
    public Logger logger;
    public MyInternalReceiver receiver;
    public MyTorqueIsQuittingReceiver torqueIsQuittingReceiver;
    public static boolean autoScrollingEnabled;
    public static MyPidMonitorService pidMonitorService;
    public static TextView txtPidName;
    public static TextView txtServiceStatus;
    public static TextView txtPidValue;
    public static TextView txtIsConnectedECU;
    public static Button btnStartStopService;
    public static Button btnPreferences;
    public static TextView txtPidLastUpdated;
    public static ScrollView mScrollView;
    public static ToggleButton toggleButton;
    public static ImageView broadCastImage;
    public static TextView txtLog;
    public static String X_POS = "X_POS";
    public static String Y_POS = "Y_POS";
    public static int scrollX = 0;
    public static int scrollY = -1;
    public static boolean userIsChoosingPID = false;
    public static final String LOGTEXT = "LOGTEXT";
    public static final String IS_AUTOSCROLLING = "IS_AUTOSCROLLING";
    public Drawable img_right_play;
    public Drawable img_right_pause;
    private static boolean isAutoscrolling;
    public static final String INTENT_ACTION_QUIT = "QUIT_ACTIVITY";
    public static boolean quitRequested = false;
    public GestureDetectorCompat mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        options = new MySettingsHelper(this);
        setContentView(R.layout.activity_main);
        quitRequested = false;
        if (this.getIntent() != null) {
            if (this.getIntent().getAction() != null) {
                if (this.getIntent().getAction().equals(INTENT_ACTION_QUIT)) {
                    Log.w(TAG, "onCreate: RECEIVED A QUIT APP INTENT - LEAVING!");
                    quitRequested = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        finishAffinity();
                    }
                    return;
                }
            }
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);

        options.serviceAllowedToRun(true);
        img_right_play = ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause);
        img_right_pause = ContextCompat.getDrawable(this, android.R.drawable.ic_media_play);
        broadCastImage = (ImageView) findViewById(R.id.imageViewSending);

        mGestureDetector = new GestureDetectorCompat(this, this);


        if (logger == null) {
            logger = new Logger(this);
        }

        torqueIsQuittingReceiver = new MyTorqueIsQuittingReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                super.onReceive(context, intent);
                MyPidMonitorService.quitPending = true;
                stopService(new Intent(getApplicationContext(), MyPidMonitorService.class));
            }
        };
        receiver = new MyInternalReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                super.onReceive(context, intent);
                if (intent.getAction().equals(BROADCAST_INTENT_FILTER)) {
                    String text = intent.getStringExtra(EXTRA_KEY_APPEND_LOG);

                    doBroadcastAnimation();

                    Logger.LogEvent logEvent = new Logger.LogEvent(text);
                    writeToLog(logEvent);

                    if (MyPidMonitorService.connectedToECU) {
                        txtIsConnectedECU.setTextColor(Color.parseColor("#BABABA"));
                        txtIsConnectedECU.setText(MyPidMonitorService.ECU_CONNECTED);
                    } else {
                        txtIsConnectedECU.setTextColor(Color.RED);
                        txtIsConnectedECU.setText(MyPidMonitorService.ECU_NOT_CONNECTED);
                    }
                    if (MyPidMonitorService.serviceRunning) {
                        txtServiceStatus.setTextColor(Color.parseColor("#BABABA"));
                        txtServiceStatus.setText(MyPidMonitorService.SERVICE_IS_ON_TEXT);
                    } else {
                        txtServiceStatus.setTextColor(Color.RED);
                        PluginActivity.txtServiceStatus.setText(MyPidMonitorService.SERVICE_IS_OFF_TEXT);
                    }
                }
            }
        };
        txtPidName = (TextView) findViewById(R.id.textViewPidBeingMonitored);

        txtServiceStatus = (TextView) findViewById(R.id.textView_serviceStatus);
        txtPidValue = (TextView) findViewById(R.id.textView_EngineLoadValue);
        txtIsConnectedECU = (TextView) findViewById(R.id.textView_ECU_status);
        txtPidLastUpdated = (TextView) findViewById(R.id.textViewPIDlastUpdated);
        mScrollView = (ScrollView) findViewById(R.id.SCROLLER_ID);
        mScrollView.setOnTouchListener(myTouchListener);

        txtLog = (TextView) findViewById(R.id.TEXT_LOG);
        txtLog.setTextColor(Color.parseColor("#FFBB00"));
        toggleButton = (ToggleButton) findViewById(R.id.toggleButtonAutoScroll);

        btnStartStopService = (Button) findViewById(R.id.buttonDisconnect);
        btnStartStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStartStopService.setEnabled(false);
                startStopService();
            }
        });

        if (options.serviceAllowedToRun() && ! MyPidMonitorService.quitPending) {
            Intent intent = new Intent(this, MyPidMonitorService.class);
            intent.putExtra(MyPidMonitorService.INTENT_EXTRA_START_SERVICE, true);
            startService(intent);
        } else {
            btnStartStopService.setText("Start Service");
        }

        btnPreferences = (Button) findViewById(R.id.buttonPrefs);
        btnPreferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userIsChoosingPID = true;
                startStopService(!userIsChoosingPID);
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        if (pidMonitorService == null && ! quitRequested) {
            pidMonitorService = new MyPidMonitorService();
        }

        if (txtLog.length() < 1) {
            txtLog.setText("Talking to broadcaster, please wait...");
        }

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleScroll(toggleButton.isChecked());
                Log.i(TAG, "onClick: isChecked: " + toggleButton.isChecked());
            }

        });

        autoScrollingEnabled=true;
        toggleButton.setChecked(autoScrollingEnabled);

        if (savedInstanceState != null) {
            try {
                autoScrollingEnabled = savedInstanceState.getBoolean("IS_AUTOSCROLLING");

                toggleButton.setChecked(savedInstanceState.getBoolean("TOGGLE_BUTTON_STATE"));
                toggleButton.setTextColor(savedInstanceState.getInt("TOGGLE_BUTTON_TEXT_COLOR"));

                txtServiceStatus.setText(savedInstanceState.getString("SERVICE_TEXT"));
                txtServiceStatus.setTextColor(savedInstanceState.getInt("SERVICE_TEXT_COLOR"));

                txtIsConnectedECU.setText(savedInstanceState.getString("ECU_TEXT"));
                txtIsConnectedECU.setTextColor(savedInstanceState.getInt("ECU_TEXT_COLOR"));

                isAutoscrolling = autoScrollingEnabled;

                if (isAutoscrolling) {
                    logger.clear();
                    List<Logger.LogEvent> allEvents = savedInstanceState.getParcelableArrayList("LOG_ENTRIES");
                    logger.addAll(allEvents);
                    scrollToBottom();
                    toggleScroll(true);
                } else {
                    txtLog.setText(savedInstanceState.getString("LOG_TEXT"));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        mScrollView.setScrollY(savedInstanceState.getInt("SCROLL_Y_POS"));
                    }
                }

            } catch (Exception e) {
                Log.w(TAG, "FAILED TO RESTORE VALUES FROM SAVEDINSTANCESTATE!!");
                e.printStackTrace();
            }
        } else {
            scrollToBottom();
            toggleScroll(true);
            txtServiceStatus.setText("Talking to broadcaster, please wait...");
            txtIsConnectedECU.setText("Talking to broadcaster, please wait...");
            txtServiceStatus.setTextColor(Color.WHITE);
            txtIsConnectedECU.setTextColor(Color.WHITE);
        }
    }

    private void doBroadcastAnimation() {
        int duration = options.getFrequency() / 4;
        broadCastImage.setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                broadCastImage.setVisibility(View.INVISIBLE);
                Log.i(TAG, "run: Hid the broadcast icon");
            }
        };
        handler.postDelayed(runner, duration);
    }

    View.OnTouchListener myTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            return false;
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    View.OnScrollChangeListener getOnScrollChangeListener() {
        View.OnScrollChangeListener onScrollChangeListener = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            onScrollChangeListener = new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View view, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if (isAutoscrolling) return;
                    int thresh = 25;
                    int reenableThresh = 100;
                    int o = oldScrollY;
                    int n = scrollY;
                    int mScrollBottom = mScrollView.getBottom();
                    int txtLogBottom = txtLog.getBottom();
                    Log.i(TAG, "onScrollChange: old: " + o + "\nnew: " + n + "\nmScrollBottom: " + mScrollBottom + "\ntxtLogBottom: " + txtLogBottom + "\ncombined: " + mScrollBottom + txtLogBottom + "/" + n);
                    if ((o > n) && (Math.abs(o - n) >= thresh ) && (autoScrollingEnabled) && ! (n <= 1)) {
                        toggleButton.performClick();
                        Toast.makeText(getApplicationContext(), "Scrolling disabled",
                                Toast.LENGTH_SHORT).show();
                    }

                }
            };
        }
        return onScrollChangeListener;
    }

    private void toggleScroll(boolean checked) {
        autoScrollingEnabled = checked;
        if (checked) {
            scrollToBottom();
            // toggleButton.setTextColor(Color.parseColor("#BABABA"));
            //toggleButton.setText("Logging on");
        } else {
            // toggleButton.setTextColor(Color.RED);
            //toggleButton.setText("Logging (paused)");
        }
    }

    private void writeToLog(Logger.LogEvent logEvent) {

        if (logger == null) {
            logger = new Logger(getApplicationContext());
        }
        logger.append(logEvent);
        draw();

        /*final Animation in = new AlphaAnimation(0.9f, 1.0f);
        in.setDuration(options.getFrequency() / 10);
        final Animation out = new AlphaAnimation(1.0f, 0.9f);
        out.setDuration(options.getFrequency() / 10);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (autoScrollingEnabled) {
                txtLog.startAnimation(out);
            }
        }

        out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                draw();
                txtLog.startAnimation(in);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });*/

    }

    private void draw() {
        if (MyPidMonitorService.connectedToECU) {
            txtIsConnectedECU.setTextColor(Color.parseColor("#BABABA"));
            txtIsConnectedECU.setText(MyPidMonitorService.ECU_CONNECTED);
        } else {
            txtIsConnectedECU.setTextColor(Color.RED);
            txtIsConnectedECU.setText(MyPidMonitorService.ECU_NOT_CONNECTED);
        }

        if (autoScrollingEnabled) {
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                txtLog.setText(Html.fromHtml(logger.write(), Html.FROM_HTML_MODE_LEGACY));
            } else {
                txtLog.setText(Html.fromHtml(logger.write()));
            }
            scrollToBottom();
        } else {
            Log.w(TAG, "writeToLog: Didn't refresh txtLog as autoscrolling is off.");
        }
    }

    private void scrollToBottom() {
        isAutoscrolling = true;
        try {
            mScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mScrollView.smoothScrollTo(0, txtLog.getBottom());
                    mScrollView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isAutoscrolling = false;
                        }
                    },300);
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "FAILED TO SCROLL TEXT");
            e.printStackTrace();
            isAutoscrolling = false;
        }
    }

    @Override
    protected void onPause() {

        try {
            unregisterReceiver(receiver);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mScrollView.setOnScrollChangeListener(null);
            }
            // unregisterReceiver(torqueIsQuittingReceiver);
        } catch (Exception e) {
            Log.d(TAG, "");

            e.printStackTrace();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {

        if (! quitRequested) {
            registerReceiver(receiver, MyInternalReceiver.intentFilter);
            // registerReceiver(torqueIsQuittingReceiver, MyTorqueIsQuittingReceiver.intentFilter);
            if (autoScrollingEnabled) {
                scrollToBottom();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mScrollView.setOnScrollChangeListener(getOnScrollChangeListener());
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                finishAffinity();
            } else {
                finish();
            }
        }
        super.onResume();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        quitRequested = false;
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mScrollView.setOnScrollChangeListener(null);
            }
        } catch (Exception e) {
            Log.d(TAG, "Null'd the onscroll listener");
            e.printStackTrace();
        }

        outState.putBoolean("TOGGLE_BUTTON_STATE", toggleButton.isChecked());
        outState.putInt("TOGGLE_BUTTON_TEXT_COLOR", toggleButton.getCurrentTextColor());
        outState.putBoolean("IS_AUTOSCROLLING", autoScrollingEnabled);
        outState.putString("SERVICE_TEXT", txtServiceStatus.getText().toString());
        outState.putString("ECU_TEXT", txtIsConnectedECU.getText().toString());
        outState.putInt("ECU_TEXT_COLOR", txtIsConnectedECU.getCurrentTextColor());
        outState.putInt("SERVICE_TEXT_COLOR", txtServiceStatus.getCurrentTextColor());
        outState.putString("LOG_TEXT", txtLog.getText().toString());
        outState.putInt("SCROLL_Y_POS", mScrollView.getScrollY());
        ArrayList<Logger.LogEvent> allLogEvents = new ArrayList<>();
        allLogEvents.addAll(logger.getAll());
        outState.putParcelableArrayList("LOG_ENTRIES", allLogEvents);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate userSwipedDown one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                onBackPressed();
                return true;
            case R.id.action_preferences:
                Intent intent = new Intent(getApplicationContext(), PreferencesActivity.class);
                startActivityForResult(intent, PREFERENCES_REQUEST_CODE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==PREFERENCES_REQUEST_CODE) {
            switch (resultCode) {
                case(PreferencesActivity.RESULT_OKAY) :

                break;
                case(PreferencesActivity.RESULT_NOT_OKAY) :

                    break;
                case(PreferencesActivity.RESULT_NEED_RESTART) :

                    break;
                default:
            }
        }
    }

    public void startService() {

        if (quitRequested) {
            return;
        }

        if (pidMonitorService == null) {
            pidMonitorService = new MyPidMonitorService();
        }
        if (! MyPidMonitorService.quitPending) {
            Intent intent = new Intent(getApplicationContext(), MyPidMonitorService.class);
            intent.putExtra(MyPidMonitorService.INTENT_EXTRA_START_SERVICE,true);
            startService(intent);
        }
    }

    public void startStopService(boolean startStop) {

        if (quitRequested) {
            return;
        }

        if (startStop == false) {
            Toast.makeText(getApplicationContext(), "Stopping broadcaster...", Toast.LENGTH_SHORT).show();
            options.serviceAllowedToRun(false);
            if (btnStartStopService != null) {
                btnStartStopService.setText("Stopping...");
            }
            pidMonitorService.stop();
        } else {
            Toast.makeText(getApplicationContext(), "Starting broadcaster...", Toast.LENGTH_SHORT).show();
            options.serviceAllowedToRun(true);
            if (btnStartStopService != null) {
                btnStartStopService.setText("Starting...");
            }
            startService();
        }
    }

    public void startStopService() {

        if (quitRequested) {
            return;
        }

        if (MyPidMonitorService.serviceRunning) {
            Toast.makeText(getApplicationContext(), "Stopping broadcaster...", Toast.LENGTH_SHORT).show();
            if (btnStartStopService != null) {
                btnStartStopService.setText("Stopping...");
            }
            options.serviceAllowedToRun(false);
            pidMonitorService.stop();
            unregisterReceiver(receiver);
        } else {
            Toast.makeText(getApplicationContext(), "Starting broadcaster...", Toast.LENGTH_SHORT).show();
            if (btnStartStopService != null) {
                btnStartStopService.setText("Starting...");
            }
            options.serviceAllowedToRun(true);
            startService();
            registerReceiver(receiver, MyInternalReceiver.intentFilter);
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        if (autoScrollingEnabled) return false;

            Log.w(TAG, "onFling: Negative fling detected - turning on autoscroll");
        try {
            if (velocityY <= -5000) {
                toggleButton.performClick();
                Log.w(TAG, "onFling: NEGATIVE VELOCITY: " + velocityY);
            } else if (velocityY >= 5000) {
                Log.w(TAG, "onFling: POSITIVE VELOCITY");
            }
        } catch (Exception e) {
            Log.d(TAG, "FAILED TO PROCESS FLING!");
            e.printStackTrace();
        }
        return false;
    }

}
