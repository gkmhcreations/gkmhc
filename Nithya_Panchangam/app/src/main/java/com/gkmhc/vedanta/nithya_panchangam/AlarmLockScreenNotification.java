package com.gkmhc.vedanta.nithya_panchangam;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

/**
 * Alarm Alert Notification Activity to show a full screen view with options to
 * Snooze (or) Dismiss Alarm on the Phone's lockscreen.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class AlarmLockScreenNotification extends AppCompatActivity {
    private boolean alarmType;
    private int alarmID = 0;
    private int alarmHourOfDay = 0;
    private int alarmMin = 0;
    private String ringTone;
    private int repeatOption;
    private boolean toVibrate = false;
    private String label;
    private int iconID = R.drawable.swamy_ayyappan_circle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String prefLang = MainActivity.updateSelLocale(this);
        Locale locale = new Locale(prefLang);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_lock_screen_notification);

        label = getString(R.string.splash_screen_banner);
        Intent recvdIntent = this.getIntent();
        if (recvdIntent != null) {
            alarmType = recvdIntent.getBooleanExtra(
                    NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_TYPE, Alarm.ALARM_TYPE_STANDARD);
            alarmID = recvdIntent.getIntExtra(
                    NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_ID, 0);
            alarmHourOfDay = recvdIntent.getIntExtra(
                    NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY, 0);
            alarmMin = recvdIntent.getIntExtra(
                    NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_MIN, 0);
            ringTone = recvdIntent.getStringExtra(
                    NPBroadcastReceiver.EXTRA_NOTIFICATION_RINGTONE);
            repeatOption = recvdIntent.getIntExtra(
                    NPBroadcastReceiver.EXTRA_NOTIFICATION_REPEAT, Alarm.INVALID_VALUE);
            toVibrate = recvdIntent.getBooleanExtra(
                    NPBroadcastReceiver.EXTRA_NOTIFICATION_VIBRATE, false);
            label = recvdIntent.getStringExtra(
                    NPBroadcastReceiver.EXTRA_NOTIFICATION_LABEL);
            iconID = recvdIntent.getIntExtra(
                    NPBroadcastReceiver.EXTRA_NOTIFICATION_ICON_ID, R.drawable.swamy_ayyappan_circle);
        }

        if (label.isEmpty()) {
            label = getString(R.string.splash_screen_banner);
        }

        Button snoozeButton = findViewById(R.id.lockscreen_snooze_button);
        snoozeButton.setOnClickListener(v ->
                NPBroadcastReceiver.notifyBroadcastReceiver(getApplicationContext(),
                NPBroadcastReceiver.SNOOZE_ALARM, alarmType, alarmID, alarmHourOfDay, alarmMin,
                ringTone, toVibrate, repeatOption, label, iconID));
        Button dismissButton = findViewById(R.id.lockscreen_dismiss_button);
        dismissButton.setOnClickListener(v ->
                NPBroadcastReceiver.notifyBroadcastReceiver(getApplicationContext(),
                NPBroadcastReceiver.DISMISS_ALARM, alarmType, alarmID, alarmHourOfDay, alarmMin,
                ringTone, toVibrate, repeatOption, label, iconID));

        Log.d("AlarmLockScreen","Label: " + label + " IconID: " + iconID);

        TextView labelTextView = findViewById(R.id.lockscreen_text);
        labelTextView.setText(label);

        ImageView iconImageView = findViewById(R.id.lockscreen_logo);
        iconImageView.setImageResource(iconID);
        iconImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iconImageView.setAdjustViewBounds(true);

        // Register to Broadcast receiver to be notified when Alarm Ringtone is stopped so that this
        // activity can be closed gracefully.
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(alarmMsgReceiver,
                        new IntentFilter(NPBroadcastReceiver.EXTRA_NOTIFICATION_RINGTONE_STOPPED));
    }

    /**
     * This is called by broadcast receiver after stopping ringtone successfully.
     */
    private final BroadcastReceiver alarmMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (intent.getAction().
                    equalsIgnoreCase(NPBroadcastReceiver.EXTRA_NOTIFICATION_RINGTONE_STOPPED)) {
                Log.d("AlarmLockScreen","Received Ringtone STOPPED notification!");
                finish();
            }
        }
        }
    };

    /**
     * Show this activity and do the following even when Phone is locked:
     * 1) Turn Screen On
     * 2) Keep Screen On
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }
}