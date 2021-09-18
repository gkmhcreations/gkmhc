package com.gkmhc.vedanta.nithya_panchangam;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gkmhc.utils.VedicCalendar;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Broadcast receiver to handle Alarm & Reminder related broadcast messages.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class NPBroadcastReceiver extends BroadcastReceiver {
    private static Ringtone ringTonePlayer; // Remember this to start/stop Ringtones
    private static int alarmIDForPlayingRingtone = Alarm.INVALID_VALUE;
    public static final int SNOOZE_10MINS = 10;
    public static final int SNOOZE_24HRS = (24 * 60);
    public static final String START_ALARM = "StartAlarm";
    public static final String START_ALARM_HANDLER = "AlarmStartHandler";
    public static final String STOP_ALARM = "StopAlarm";
    public static final String DISMISS_ALARM = "DismissAlarm";
    public static final String DELETE_ALARM = "DeleteAlarm";
    public static final String SNOOZE_ALARM = "SnoozeAlarm";
    public static final String EXTRA_NOTIFICATION_ALARM_TYPE = "AlarmType";
    public static final String EXTRA_NOTIFICATION_ALARM_ID = "AlarmID";
    public static final String EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY = "AlarmHourOfDay";
    public static final String EXTRA_NOTIFICATION_ALARM_MIN = "AlarmMin";
    public static final String EXTRA_NOTIFICATION_RINGTONE = "Ringtone";
    public static final String EXTRA_NOTIFICATION_VIBRATE = "Vibrate";
    public static final String EXTRA_NOTIFICATION_REPEAT = "Repeat";
    public static final String EXTRA_NOTIFICATION_RESTART = "Restart";
    public static final String EXTRA_NOTIFICATION_LABEL = "Label";
    public static final String EXTRA_NOTIFICATION_ICON_ID = "IconID";
    public static final String EXTRA_NOTIFICATION_RINGTONE_START = "StartRingTone";
    public static final String EXTRA_NOTIFICATION_RINGTONE_STOPPED = "RingToneStopped";

    /**
     * Use this API to add Alarm information to persistent DB.
     *
     * @param context   App Context
     * @param intent    Intent that contains the Alarm Information to be processed
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action;

        // Overall State Transition Diagram:
        //
        // App / User |-------->|<--------------------------------------------------------------|
        //  |         |         |                                                               |
        //  |         |         V                                                               |
        //  |         |   START_ALARM[1] ------|                                                |
        //  |         |                        |                                                |
        //  |         |                        |                                                |
        //  |         |                        V                                                |
        //  |         |        |------ AlarmManager(Android)-----------|                        |
        //  |         |        |               A                       |                        |
        //  |         |        |               |                       |                        |
        //  |         |        V               |                       |                        |
        //  |         | START_ALARM_HANDLER[2]-|                       |                        |
        //  |         |                        |                       |                        |
        //  |         |                        |                       |                        |
        //  |(Wait for user input)             |                       V                        |
        //  |---------|--------------------------------- Show Snooze & Dismiss Notification     |
        //  |         |                        |                                                |
        //  |         |                        |                                   (Goto[1])    |
        //  |---------|-> SNOOZE_ALARM[3] -----| Cancel Alarm --------------------------------->|
        //  |         |                        |                                                |
        //  |         |         (or)           |                                                |
        //  |         |                        |             (Goto[1] - Daily/Custom Repeats)   |
        //  |---------|-> DISMISS_ALARM[3] ----| Cancel Alarm --------------------------------->|
        //  |         |                        |
        //  |         |                        |
        //  |         |                        |
        //  |---------|-> STOP_ALARM[4] -------- Cancel Alarm
        //  |         |                        |
        //  |         |         (or)           |
        //  |         |                        |
        //  |---------|-> DELETE_ALARM[4] -----| Cancel Alarm
        //
        // Message-level Details:
        // [1] START_ALARM
        //     -> Register with AlarmManager to fire Alarm at the particular time (HH:MM)
        // [2] START_ALARM_HANDLER
        //     -> AlarmManager calls START_ALARM_HANDLER with Alarm Details
        //     -> Register Tap, Snooze, Dismiss & fullScreen Intents with AlarmManager
        //     -> Register Notification to show up "Snooze" & "Dismiss" options when Alarm fires
        // [3] SNOOZE_ALARM
        //     -> AlarmManager notifies this when user clicks on "Snooze" from Alarm notification
        //     -> Once/Daily/Custom -> stopRingTone -> cancelAlarm -> snoozeAlarm (10 mins) -> Go to [1]
        // [3] DISMISS_ALARM
        //     -> AlarmManager notifies this when user clicks on "Dismiss" from Alarm notification
        //     -> Once         -> stopRingTone -> cancelAlarm -> Notify App
        //     -> Daily/Custom -> stopRingTone -> cancelAlarm -> startNewAlarm(24 hrs) -> Go to [1]
        // [4] STOP_ALARM
        //     -> App notifies this when user clicks on "Dismiss" toggle in UI
        //     -> Once/Daily/Custom -> stopRingTone -> cancelAlarm -> Notify App
        // [4] DELETE_ALARM
        //     -> App notifies this when user clicks on "Delete" in UI
        //     -> Once/Daily/Custom -> stopRingTone -> cancelAlarm
        if (intent != null) {
            action = intent.getAction();
            switch (action) {
                case Intent.ACTION_BOOT_COMPLETED:
                case Intent.ACTION_DATE_CHANGED:
                case Intent.ACTION_TIME_CHANGED:
                case Intent.ACTION_TIMEZONE_CHANGED:
                    // This notification is sent to handle restart of all alarms & reminders.
                    // Scenarios:
                    // 1) Phone Restart
                    // 2) Timezone change
                    // 3) Date change
                    // 4) Time change
                    Log.i("NPBroadcastReceiver", "NPAlarm: " + action + " received!");

                    // Handle Phone Restart Scenario
                    recreateAlarmsFromPersistentDB(context.getApplicationContext());
                    recreateRemindersFromPersistentDB(context.getApplicationContext());
                    break;
                case START_ALARM:
                    // Register Alarm with AlarmManager.
                    // Scenario: User triggers or when App starts, this notification is triggered
                    //           for each Alarm.
                    // Handle Phone & App Restart Scenarios
                    startAlarm(context, intent);
                    break;
                case START_ALARM_HANDLER:
                    // Snooze Alarm by 10 mins as asked by user
                    // Scenario: User clicked Snooze button in either in pop-up notification (or)
                    //           lockscreen activity.
                    int alarmID = intent.getIntExtra(EXTRA_NOTIFICATION_ALARM_ID,
                                                     Alarm.INVALID_VALUE);
                    boolean alarmType = intent.getBooleanExtra(EXTRA_NOTIFICATION_ALARM_TYPE,
                                                               Alarm.ALARM_TYPE_STANDARD);
                    int repeatOption = intent.getIntExtra(EXTRA_NOTIFICATION_REPEAT,
                                                          Alarm.INVALID_VALUE);

                    Log.i("NPBroadcastReceiver", "NPAlarm: " + action + "(" + alarmID + ") received!");
                    if (alarmType == Alarm.ALARM_TYPE_VEDIC) {
                        List<Integer> dhinaVisheshamCodeList = findDinaVishesham(context);

                        // There is a possibility of more than one Dina Vishesham(s) occurring
                        // in a particular calendar day.
                        if (dhinaVisheshamCodeList != null) {
                            // If alarmID is present in the list of Dina Vishesham(s) for the day,
                            // then trigger Alarm.
                            // Else, restart Alarm.
                            if (dhinaVisheshamCodeList.contains(alarmID)) {
                                Log.i("NPBcastReceiver",
                                        "NPAlarm: Triggering Alarm(" + alarmID + ") for " +
                                                "Dina Vishesham: " + alarmID +
                                                " with repeat option: " + repeatOption + " !");
                                triggerAlarm(context, intent, alarmID);
                            } else {
                                Log.i("NPBcastReceiver",
                                        "NPAlarm: Skipping Alarm(" + alarmID + ") as " +
                                                "Dina Vishesham: " + alarmID +
                                                " does not match!");
                                cancelAlarm(context, intent, alarmID, true);
                            }
                        }
                    } else {
                        Calendar curCalendar = Calendar.getInstance();
                        int weekDay = curCalendar.get(Calendar.DAY_OF_WEEK);
                        weekDay -= 1; // Align to Sun(0), ... , Sat(6)
                        String dayOfWeek = curCalendar.getDisplayName(Calendar.DAY_OF_WEEK,
                                Calendar.SHORT, Locale.ENGLISH);
                        String repeatOptionsStr = String.valueOf(repeatOption);
                        String weekDayStr = String.valueOf(weekDay);

                        // Proceed only if the following conditions are met:
                        // 1) Repeat only once
                        // 2) Repeat Daily
                        // 3) Repeat custom but the current alarm is firing on a day as set by user
                        if ((repeatOption == Alarm.ALARM_REPEAT_ONCE) ||
                            (repeatOption == Alarm.ALARM_REPEAT_DAILY) ||
                            (repeatOptionsStr.contains(weekDayStr))) {
                            Log.i("NPBcastReceiver",
                                    "NPAlarm: Triggering Alarm(" + alarmID + ") on " +
                                            dayOfWeek + " for repeat option: " + repeatOption + " !");
                            triggerAlarm(context, intent, alarmID);
                        } else {
                            // Custom repeating alarm which should fire on a different day
                            // Hence, Restart same alarm but to fire 24 hours later
                            Log.i("NPBcastReceiver",
                                    "NPAlarm: Skipping Alarm(" + alarmID + ") on " +
                                            dayOfWeek + " for repeat option: " + repeatOption + " !");
                            cancelAlarm(context, intent, alarmID, true);
                        }
                    }
                    break;
                case SNOOZE_ALARM:
                    // Snooze Alarm by 10 mins as asked by user
                    // Scenario: User clicked Snooze button in either in pop-up notification (or)
                    //           lockscreen activity.
                    alarmID = intent.getIntExtra(EXTRA_NOTIFICATION_ALARM_ID,
                                                 Alarm.INVALID_VALUE);

                    Log.i("NPBroadcastReceiver", "NPAlarm: " + action + "(" + alarmID + ") received!");

                    // Stop Ringtone
                    stopRingTone(context, alarmID);

                    // Cancel Alarm & PendingIntent
                    cancelAlarm(context, intent, alarmID, false);

                    // Snooze Alarm with pre-defined increment
                    snoozeAlarm(context, intent, alarmID, SNOOZE_10MINS);
                    break;
                case DISMISS_ALARM:
                    // Dismiss the Alarm as asked by user
                    // Scenario: User clicked Dismiss button in either in pop-up notification (or)
                    //           lockscreen activity.
                    // Note: Cancel only one-time alarms. Repeat alarms should remain active.
                    alarmID = intent.getIntExtra(EXTRA_NOTIFICATION_ALARM_ID, Alarm.INVALID_VALUE);
                    alarmType = intent.getBooleanExtra(EXTRA_NOTIFICATION_ALARM_TYPE, Alarm.ALARM_TYPE_STANDARD);
                    repeatOption = intent.getIntExtra(EXTRA_NOTIFICATION_REPEAT, Alarm.INVALID_VALUE);

                    Log.i("NPBroadcastReceiver", "NPAlarm: " + action + "(" + alarmID + ") received!");

                    // Stop Ringtone
                    stopRingTone(context, alarmID);

                    // Notify App only when Repeat option is one-time.
                    if (repeatOption == Alarm.ALARM_REPEAT_ONCE) {
                        // Cancel Alarm & PendingIntent
                        // But DONT start another alarm. Following scenarios are handled here:
                        // 1) Standard Alarm - Repeat: Once
                        // 2) Panchamgam Alarm - Repeat: Once
                        cancelAlarm(context, intent, alarmID, false);

                        // Notify Main Activity.
                        // In case, main activity is on and active tab is "Alarm/Reminder", then
                        // this notification would refresh these tabs and the particular
                        // Alarm/Reminder can move to "stopped" state as per UI.
                        notifyNPMainActivity(context, STOP_ALARM, alarmID, alarmType);
                    } else {
                        // Cancel Alarm & PendingIntent
                        // But start another alarm for the following scenarios:
                        // 1) Standard Alarm - Repeat: Daily & Custom
                        // 2) Panchamgam Alarm - Repeat: Every Occurrence
                        cancelAlarm(context, intent, alarmID, true);
                    }
                    break;
                case STOP_ALARM:
                    // Stop the Alarm as asked by user.
                    // Scenario: User clicked Toggle button in App UI.
                    // Note: Cancel the alarm regardless of their repeatability.
                case DELETE_ALARM:
                    // Delete the Alarm as asked by user.
                    // Scenario: User clicked Deletion option in App UI related to the given Alarm.
                    // Note: Cancel & stop the alarm regardless of their repeatability.
                    alarmID = intent.getIntExtra(EXTRA_NOTIFICATION_ALARM_ID, Alarm.INVALID_VALUE);
                    Log.i("NPBroadcastReceiver", "NPAlarm: " + action + "(" + alarmID + ") received!");

                    // Stop Ringtone
                    stopRingTone(context, alarmID);

                    // Cancel Alarm & PendingIntent
                    cancelAlarm(context, intent, alarmID, false);
                    break;
            }
        }
    }

    /**
     * Use this utility function to register an Alarm with AlarmManager.
     * @param context       App Context
     * @param recvdIntent   Intent that contains Alarm Information
     */
    private void startAlarm(Context context, Intent recvdIntent) {
        AlarmManager alManager =
                (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (alManager != null) {

            /*
             * Alarm Information present in the received Intent is as follows:
             * alarmType         Alarm Type (Standard / Panchangam)
             * alarmID           Alarm ID
             * alarmHourOfDay    Alarm Hour of Day (24-hour format)
             * alarmMin          Alarm Minutes
             * ringTone          Full path to Ringtone
             * toVibrate         true - Vibrate, false - Do NOT Vibrate
             * repeatOption      Once   - Buzz Alarm one-time
             *                   Daily  - Buzz Alarm daily at the Alarm Hour & Minute
             *                   Custom - Buzz Alarm at the Alarm Hour & Minute on selected days
             */
            boolean alarmType = recvdIntent.getBooleanExtra(EXTRA_NOTIFICATION_ALARM_TYPE, Alarm.ALARM_TYPE_STANDARD);
            int alarmID = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_ALARM_ID, Alarm.INVALID_VALUE);
            int alarmHourOfDay = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY, 0);
            int alarmMin = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_ALARM_MIN, 0);
            String ringTone = recvdIntent.getStringExtra(EXTRA_NOTIFICATION_RINGTONE);
            boolean toVibrate = recvdIntent.getBooleanExtra(EXTRA_NOTIFICATION_VIBRATE, false);
            int repeatOption = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_REPEAT, Alarm.INVALID_VALUE);
            boolean forceRestart = recvdIntent.getBooleanExtra(EXTRA_NOTIFICATION_RESTART, false);
            String label = recvdIntent.getStringExtra(EXTRA_NOTIFICATION_LABEL);
            int iconID = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_ICON_ID, R.drawable.swamy_ayyappan_circle);

            Log.i("NPBroadcastReceiver", "NPAlarm: " + START_ALARM + "(" + alarmID + ") received!");

            // Scenarios to handle:
            // 1) If force Alarm restart is true then create alarm with same alarmID
            //    --> This should restart alarm!
            // 2) If force restart is false, then
            //    - If alarm is NOT running, then create new alarm
            //    - Else ignore
            if ((forceRestart) || (isAlarmOff(context, alarmID))) {
                Calendar curCalendar = Calendar.getInstance();
                int curCalendarHour = curCalendar.get(Calendar.HOUR_OF_DAY);
                int curCalendarMin = curCalendar.get(Calendar.MINUTE);

                // Change date to next day if any of the below conditions match:
                // 1) If alarm hour is less than current hour
                // 2) If alarm minutes is less than current minute
                if (curCalendarHour > alarmHourOfDay) {
                    curCalendar.add(Calendar.DATE, 1);
                } else if ((curCalendarHour == alarmHourOfDay) && (curCalendarMin >= alarmMin)) {
                    curCalendar.add(Calendar.DATE, 1);
                }

                curCalendar.set(Calendar.HOUR_OF_DAY, alarmHourOfDay);
                curCalendar.set(Calendar.MINUTE, alarmMin);
                curCalendar.set(Calendar.SECOND, 0);

                int alarmDate = curCalendar.get((Calendar.DATE));
                int alarmMonth = curCalendar.get((Calendar.MONTH));
                int alarmYear = curCalendar.get((Calendar.YEAR));
                Intent startIntent = new Intent(context, NPBroadcastReceiver.class);
                startIntent.putExtra(EXTRA_NOTIFICATION_ALARM_TYPE, alarmType);
                startIntent.putExtra(EXTRA_NOTIFICATION_ALARM_ID, alarmID);
                startIntent.putExtra(EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY, alarmHourOfDay);
                startIntent.putExtra(EXTRA_NOTIFICATION_ALARM_MIN, alarmMin);
                startIntent.putExtra(EXTRA_NOTIFICATION_RINGTONE, ringTone);
                startIntent.putExtra(EXTRA_NOTIFICATION_VIBRATE, toVibrate);
                startIntent.putExtra(EXTRA_NOTIFICATION_REPEAT, repeatOption);
                startIntent.putExtra(EXTRA_NOTIFICATION_LABEL, label);
                startIntent.putExtra(EXTRA_NOTIFICATION_ICON_ID, iconID);
                startIntent.setAction(START_ALARM_HANDLER);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmID,
                        startIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                long alarmDuration = curCalendar.getTimeInMillis();
                alManager.setExact(AlarmManager.RTC_WAKEUP, alarmDuration, pendingIntent);
                Log.i("NPBroadcastReceiver",
                        "Starting Exact Alarm(" + alarmID + ") to fire on " +
                                alarmDate + "/" + (alarmMonth + 1) + "/" + alarmYear + " " +
                                alarmHourOfDay + ":" + alarmMin + " for " + alarmDuration +
                                " ms, with Ringtone: " + ringTone + " Repeat: " +
                                repeatOption + " !");
                NPDB.updateAlarmStateInDB(context, alarmType, alarmID, Alarm.ALARM_STATE_ON);
            }
        }
    }

    /**
     * Use this utility function to register the Alarm-specific notifications with AlarmManager.
     * AlarmManager then notifies the user (Snooze/Dismiss) options to handle the Alarm.
     *
     * User needs to be facilitated with following options:
     * Notification: When Alarm fires, display a notification to user to tap/snooze/dismiss Alarm.
     *  - Tap:      When user taps the notification, launch Main App
     *  - Snooze:   When user clicks on "snooze" then snooze the alarm by 10mins
     *  - Dismiss:  When user clicks on "snooze" then take any of the following actions:
     *              Repeat Once   : Stop Alarm
     *              Repeat Daily  : Stop Alarm, Start new alarm to fire in 24 hours
     *              Repeat Custom : Stop Alarm, Start new alarm to fire in 24 hours
     *  - Full Screen Intent: This is to to wake user & show a full screen pop-up notification even
     *                        when Phone is in sleep (or) lock modes.
     *
     * @param context           App Context
     * @param recvdIntent       Intent that contains Alarm Information
     * @param alarmID           Alarm ID
     */
    private void triggerAlarm(Context context, Intent recvdIntent, int alarmID) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmID,
                recvdIntent, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {

            boolean alarmType = recvdIntent.getBooleanExtra(EXTRA_NOTIFICATION_ALARM_TYPE, Alarm.ALARM_TYPE_STANDARD);
            int alarmHourOfDay = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY, 0);
            int alarmMin = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_ALARM_MIN, 0);
            String ringTone = recvdIntent.getStringExtra(EXTRA_NOTIFICATION_RINGTONE);
            boolean toVibrate = recvdIntent.getBooleanExtra(EXTRA_NOTIFICATION_VIBRATE, false);
            int repeatOption = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_REPEAT, Alarm.INVALID_VALUE);
            String label = recvdIntent.getStringExtra(EXTRA_NOTIFICATION_LABEL);
            int iconID = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_ICON_ID, R.drawable.swamy_ayyappan_circle);

            boolean isRingToneAlreadyPlaying = playRingToneAndVibration(context, alarmID, ringTone,
                                                                        toVibrate);
            if (isRingToneAlreadyPlaying) {
                Log.d("NPBroadcastReceiver", "NOT Playing RingTone for " +
                        "AlarmID: " + alarmID + " Status: " + EXTRA_NOTIFICATION_RINGTONE_START +
                        " Ringtone: " + ringTone + " Vibrate: " + toVibrate + " Repeat: " + repeatOption);
                cancelAlarm(context, recvdIntent, alarmID, false);
                snoozeAlarm(context, recvdIntent, alarmID, SNOOZE_10MINS);
            } else {
                Log.d("NPBroadcastReceiver", "Playing RingTone for " +
                        "AlarmID: " + alarmID + " Status: " + EXTRA_NOTIFICATION_RINGTONE_START +
                        " Ringtone: " + ringTone + " Vibrate: " + toVibrate + " Repeat: " + repeatOption);

                // 2) Create 4 intents:
                //    - 1st to launch MainActivity when tapped
                //    - 2nd to register a callback with Notifications to dismiss Alarm
                //    - 3rd to register a callback with Notifications to snooze Alarm
                //    - 4th to register a callback with Notifications to show lock screen

                // This is needed so that Alarm texts & notifications display texts in the
                // preferred language.
                MainActivity.updateSelLocale(context.getApplicationContext());

                // Retrieve the "Reminder" label as per latest locale selection!
                if (alarmType == Alarm.ALARM_TYPE_VEDIC) {
                    label = context.getString(Reminder.getDinaVisheshamLabel(alarmID));
                }

                //    - 1st to launch MainActivity when tapped
                Intent tapIntent = new Intent(context, MainActivity.class);
                tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent tapPendingIntent = PendingIntent.getActivity(context,
                        0, tapIntent, 0);

                //    - 2nd to register a callback with Notifications to dismiss Alarm
                Intent dismissIntent = new Intent(context, NPBroadcastReceiver.class);
                dismissIntent.setType(String.valueOf(alarmID));
                dismissIntent.setAction(DISMISS_ALARM);
                dismissIntent.putExtra(EXTRA_NOTIFICATION_ALARM_TYPE, alarmType);
                dismissIntent.putExtra(EXTRA_NOTIFICATION_ALARM_ID, alarmID);
                dismissIntent.putExtra(EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY, alarmHourOfDay);
                dismissIntent.putExtra(EXTRA_NOTIFICATION_ALARM_MIN, alarmMin);
                dismissIntent.putExtra(EXTRA_NOTIFICATION_RINGTONE, ringTone);
                dismissIntent.putExtra(EXTRA_NOTIFICATION_VIBRATE, toVibrate);
                dismissIntent.putExtra(EXTRA_NOTIFICATION_REPEAT, repeatOption);
                dismissIntent.putExtra(EXTRA_NOTIFICATION_LABEL, label);
                dismissIntent.putExtra(EXTRA_NOTIFICATION_ICON_ID, iconID);
                PendingIntent dismissPendingIntent =
                        PendingIntent.getBroadcast(context, 0, dismissIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                //    - 3rd to register a callback with Notifications to snooze Alarm
                Intent snoozeIntent = new Intent(context, NPBroadcastReceiver.class);
                snoozeIntent.setType(String.valueOf(alarmID));
                snoozeIntent.setAction(SNOOZE_ALARM);
                snoozeIntent.putExtra(EXTRA_NOTIFICATION_ALARM_TYPE, alarmType);
                snoozeIntent.putExtra(EXTRA_NOTIFICATION_ALARM_ID, alarmID);
                snoozeIntent.putExtra(EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY, alarmHourOfDay);
                snoozeIntent.putExtra(EXTRA_NOTIFICATION_ALARM_MIN, alarmMin);
                snoozeIntent.putExtra(EXTRA_NOTIFICATION_RINGTONE, ringTone);
                snoozeIntent.putExtra(EXTRA_NOTIFICATION_VIBRATE, toVibrate);
                snoozeIntent.putExtra(EXTRA_NOTIFICATION_REPEAT, repeatOption);
                snoozeIntent.putExtra(EXTRA_NOTIFICATION_LABEL, label);
                snoozeIntent.putExtra(EXTRA_NOTIFICATION_ICON_ID, iconID);
                PendingIntent snoozePendingIntent =
                        PendingIntent.getBroadcast(context, 0, snoozeIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                //    - 4th to register a callback with Notifications to show lock screen
                Intent fullScreenIntent = new Intent(context, AlarmLockScreenNotification.class);
                fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                fullScreenIntent.setType(String.valueOf(alarmID));
                fullScreenIntent.putExtra(EXTRA_NOTIFICATION_ALARM_TYPE, alarmType);
                fullScreenIntent.putExtra(EXTRA_NOTIFICATION_ALARM_ID, alarmID);
                fullScreenIntent.putExtra(EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY, alarmHourOfDay);
                fullScreenIntent.putExtra(EXTRA_NOTIFICATION_ALARM_MIN, alarmMin);
                fullScreenIntent.putExtra(EXTRA_NOTIFICATION_RINGTONE, ringTone);
                fullScreenIntent.putExtra(EXTRA_NOTIFICATION_VIBRATE, toVibrate);
                fullScreenIntent.putExtra(EXTRA_NOTIFICATION_REPEAT, repeatOption);
                fullScreenIntent.putExtra(EXTRA_NOTIFICATION_LABEL, label);
                fullScreenIntent.putExtra(EXTRA_NOTIFICATION_ICON_ID, iconID);
                PendingIntent fullScreenPendingIntent =
                        PendingIntent.getActivity(context, 0,
                                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                // Show Notification on NP channel ID
                String notifText = getNotificationText(context, alarmType, label);
                NotificationCompat.Builder notification =
                        new NotificationCompat.Builder(context, MainActivity.NP_CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_ringtone)
                                .setContentTitle(context.getString(R.string.nithya_panchangam_header))
                                .setContentText(notifText)
                                .setContentIntent(tapPendingIntent)
                                .setFullScreenIntent(fullScreenPendingIntent, true)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setCategory(NotificationCompat.CATEGORY_ALARM)
                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                .setOngoing(true)
                                .addAction(R.drawable.swamy_ayyappan_circle,
                                        context.getString(R.string.snooze_alarm), snoozePendingIntent)
                                .addAction(R.drawable.swamy_ayyappan_circle,
                                        context.getString(R.string.dismiss_alarm), dismissPendingIntent);

                NotificationManagerCompat notificationManager =
                        NotificationManagerCompat.from(context);
                notificationManager.notify(alarmID, notification.build());
            }
        } else {
            Log.i("NPBroadcastReceiver", alarmID +
                    " NOT ACTIVE. Not playing Ringtone!");
        }
    }

    /**
     * Use this utility function to snooze an Alarm by 10 mins.
     *
     * @param context           App Context
     * @param recvdIntent       Intent that contains Alarm Information
     * @param alarmID           Alarm ID
     * @param snoozeDuration    Duration to Snooze (in minutes)
     */
    private void snoozeAlarm(Context context, Intent recvdIntent, int alarmID, int snoozeDuration) {
        boolean alarmType = recvdIntent.getBooleanExtra(EXTRA_NOTIFICATION_ALARM_TYPE, Alarm.ALARM_TYPE_STANDARD);
        String ringTone = recvdIntent.getStringExtra(EXTRA_NOTIFICATION_RINGTONE);
        boolean toVibrate = recvdIntent.getBooleanExtra(EXTRA_NOTIFICATION_VIBRATE, false);
        int repeatOption = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_REPEAT, Alarm.INVALID_VALUE);
        String label = recvdIntent.getStringExtra(EXTRA_NOTIFICATION_LABEL);
        int iconID = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_ICON_ID, R.drawable.swamy_ayyappan_circle);

        // Restart same alarm to ring after snooze duration
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, snoozeDuration);
        int alarmHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int alarmMin = calendar.get(Calendar.MINUTE);

        Log.i("NPBroadcastReceiver", "Snoozing Alarm(" + alarmID + ")!!!");
        notifyBroadcastReceiver(context, START_ALARM, alarmType, alarmID, alarmHourOfDay, alarmMin,
                ringTone, toVibrate, repeatOption, label, iconID);
    }

    /**
     * Use this utility function to snooze an Alarm by 10 mins.
     *
     * @param context           App Context
     * @param recvdIntent       Intent that contains Alarm Information
     * @param alarmID           Alarm ID
     * @param restartAlarm      true - to force Restart Alarm (or) false, do nothing otherwise.
     */
    private void cancelAlarm(Context context, Intent recvdIntent, int alarmID, boolean restartAlarm) {
        AlarmManager alManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alManager != null) {
            boolean alarmType = recvdIntent.getBooleanExtra(EXTRA_NOTIFICATION_ALARM_TYPE, Alarm.ALARM_TYPE_STANDARD);

            // Stop the alarm regardless of the repeat option!
            Intent stopIntent = new Intent(context, NPBroadcastReceiver.class);
            stopIntent.setAction(START_ALARM_HANDLER);
            PendingIntent stopPendingIntent =
                    PendingIntent.getBroadcast(context, alarmID, stopIntent,
                            PendingIntent.FLAG_NO_CREATE);
            if (stopPendingIntent != null) {
                alManager.cancel(stopPendingIntent);
                stopPendingIntent.cancel();
            }

            if (isAlarmOff(context, alarmID)) {
                NPDB.updateAlarmStateInDB(context, alarmType, alarmID, Alarm.ALARM_STATE_OFF);
                Log.i("NPBroadcastReceiver",
                        "NPAlarm: Alarm(" + alarmID + ") STOPPED SUCCESSFULLY!");
            }

            int repeatOption = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_REPEAT,
                                                       Alarm.INVALID_VALUE);
            if (restartAlarm) {
                if (alarmType == Alarm.ALARM_TYPE_STANDARD) {
                    if (repeatOption == Alarm.ALARM_REPEAT_ONCE) {
                        Log.i("NPBcastReceiver",
                                "NPAlarm: Alarm(" + alarmID + ") CANCELLED!");
                    } else {
                        Log.i("NPBcastReceiver", "NPAlarm: Alarm(" + alarmID +
                                ") cancelled but RESTARTING REPEAT alarm!");
                        // Start Another alarm in case of repeat options (Daily/Custom)
                        SnoozeAlarmFor24hrs(context, recvdIntent);
                    }
                } else if (alarmType == Alarm.ALARM_TYPE_VEDIC) {
                    Log.i("NPBcastReceiver", "NPAlarm: Reminder(" + alarmID +
                            ") cancelled but RESTARTING PANCHANGAM reminder!");
                    // Start Another alarm in case of repeat options (Every Occurrence)
                    SnoozeAlarmFor24hrs(context, recvdIntent);
                }
            } else {
                Log.i("NPBcastReceiver",
                        "NPAlarm: Alarm(" + alarmID + ") CANCELLED & STOPPED!");
            }
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(alarmID);
        Log.i("NPBroadcastReceiver",
                "NPAlarm: Alarm(" + alarmID + ") notification cancelled!");
    }

    /**
     * Use this utility function to snooze an Alarm by 24 hours.
     *
     * @param context           App Context
     * @param recvdIntent       Intent that contains Alarm Information
     */
    private void SnoozeAlarmFor24hrs(Context context, Intent recvdIntent) {
        int alarmID = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_ALARM_ID, Alarm.INVALID_VALUE);
        int alarmHourOfDay = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY, 0);
        int alarmMin = recvdIntent.getIntExtra(EXTRA_NOTIFICATION_ALARM_MIN, 0);

        Log.i("NPBroadcastReceiver",
                "Restarting Alarm(" + alarmID + ") " + alarmHourOfDay + ":" + alarmMin);

        snoozeAlarm(context, recvdIntent, alarmID, SNOOZE_24HRS);
    }

    /**
     * Use this utility function to snooze an Alarm by 24 hours.
     *
     * @param context  App Context
     * @param alarmID  Alarm ID for which Ringtone is being stopped
     */
    private void stopRingTone(Context context, int alarmID) {
        stopRingToneAndVibration(context, alarmID);

        // Inform MainActivity that Ringtone & Alarm notifications have stopped.
        // UI can be updated accordingly.
        notifyNPMainActivity(context, EXTRA_NOTIFICATION_RINGTONE_STOPPED, alarmID,
                             Alarm.ALARM_TYPE_STANDARD);
        Log.d("NPBroadcastReceiver", "Stopping RingToneService");
    }

    /**
     * Use this utility function to find if an Alarm is Off.
     *
     * @param context  App Context
     * @param alarmID  Alarm ID
     *
     * @return true if Alarm is running, false otherwise.
     */
    private boolean isAlarmOff(Context context, int alarmID) {
        Intent intent = new Intent(context, NPBroadcastReceiver.class);
        intent.putExtra(EXTRA_NOTIFICATION_ALARM_ID, alarmID);
        intent.setAction(START_ALARM_HANDLER);
        return ((PendingIntent.getBroadcast(context,
                alarmID, intent, PendingIntent.FLAG_NO_CREATE)) == null);
    }

    /**
     * Use this utility function to recreate all alarms during Phone Restart.
     *
     * @param context  App Context
     */
    private void recreateAlarmsFromPersistentDB(Context context) {
        HashMap<Integer, NPDB.AlarmInfo> alarmsDB = NPDB.readAlarmsFromDB(
                context.getApplicationContext(), Alarm.ALARM_TYPE_STANDARD);
        ArrayList<Integer> alarmsList = NPDB.getAlarmIDs(alarmsDB);
        if (alarmsList != null) {
            int alarmIter = 0;
            int numAlarms = alarmsList.size();

            while (alarmIter < numAlarms) {
                int alarmID = alarmsList.get(alarmIter);
                NPDB.AlarmInfo alarmInfo = alarmsDB.get(alarmID);

                if (alarmInfo != null) {
                    boolean alarmType = alarmInfo.alarmType;
                    boolean isAlarmOn = alarmInfo.isAlarmOn;
                    int alarmHourOfDay = alarmInfo.alarmHourOfDay;
                    int alarmMin = alarmInfo.alarmMin;
                    String ringTone = alarmInfo.ringTone;
                    boolean toVibrate = alarmInfo.toVibrate;
                    int repeatOption = alarmInfo.repeatOption;
                    String label = alarmInfo.label;

                    // Recreate Alarm with the parsed fields only if Alarm State is "On"
                    if (isAlarmOn == Alarm.ALARM_STATE_ON) {
                        notifyBroadcastReceiver(context, START_ALARM, alarmType, alarmID,
                                alarmHourOfDay, alarmMin, ringTone, toVibrate, repeatOption,
                                label, Alarm.DEF_ICON_ID);
                    }
                }
                alarmIter += 1;
            }
        }
    }

    /**
     * Use this utility function to recreate all reminders during Phone Restart.
     *
     * @param context  App Context
     */
    private void recreateRemindersFromPersistentDB(Context context) {
        HashMap<Integer, NPDB.AlarmInfo> remindersDB = NPDB.readAlarmsFromDB(
                context.getApplicationContext(), Alarm.ALARM_TYPE_VEDIC);
        if (remindersDB != null) {
            ArrayList<Integer> remindersList = NPDB.getAlarmIDs(remindersDB);
            if (remindersList != null) {
                int reminderIter = 0;
                int numReminders = remindersList.size();

                while (reminderIter < numReminders) {
                    int reminderID = remindersList.get(reminderIter);
                    NPDB.AlarmInfo reminderInfo = remindersDB.get(reminderID);

                    if (reminderInfo != null) {
                        boolean reminderType = reminderInfo.alarmType;
                        boolean isReminderOn = reminderInfo.isAlarmOn;
                        int reminderHourOfDay = reminderInfo.alarmHourOfDay;
                        int reminderMin = reminderInfo.alarmMin;
                        String ringTone = reminderInfo.ringTone;
                        boolean toVibrate = reminderInfo.toVibrate;
                        int repeatOption = reminderInfo.repeatOption;
                        String label = reminderInfo.label;
                        int iconID = Reminder.getDinaVisheshamImg(reminderID);

                        // Recreate Reminder with the parsed fields only if Alarm State is "On"
                        if (isReminderOn == Alarm.ALARM_STATE_ON) {
                            notifyBroadcastReceiver(context, START_ALARM, reminderType, reminderID,
                                    reminderHourOfDay, reminderMin, ringTone, toVibrate, repeatOption,
                                    label, iconID);
                        }
                    }
                    reminderIter += 1;
                }
            }
        }
    }

    /**
     * Use this utility function to broadcast a message to Main Activity about a particular Alarm.
     *
     * @param context           App Context
     * @param actionMessage     Message indicating action to be taken
     * @param alarmID           Alarm ID
     */
    private void notifyNPMainActivity(Context context, String actionMessage, int alarmID,
                                      boolean alarmType) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(actionMessage);
        intent.putExtra(EXTRA_NOTIFICATION_ALARM_ID, alarmID);
        intent.putExtra(EXTRA_NOTIFICATION_ALARM_TYPE, alarmType);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Use this API to broadcast a message to this App.
     *
     * @param context           App Context
     * @param alarmID           Alarm ID
     * @param alarmHourOfDay    Alarm Hour of Day (24-hour format)
     * @param alarmMin          Alarm Minutes
     * @param ringTone          Full path to Ringtone
     * @param toVibrate         true - Vibrate, false - Do NOT Vibrate
     * @param repeatOption      Once   - Buzz Alarm one-time
     *                          Daily  - Buzz Alarm daily at the Alarm Hour & Minute
     *                          Custom - Buzz Alarm at the Alarm Hour & Minute on selected days
     */
    public static void notifyBroadcastReceiver(Context context, String action, boolean alarmType,
                                               int alarmID, int alarmHourOfDay, int alarmMin,
                                               String ringTone, boolean toVibrate,
                                               int repeatOption, String label, int iconID) {
        Intent intent = new Intent(context, NPBroadcastReceiver.class);
        intent.setType(String.valueOf(alarmID));
        intent.setAction(action);
        intent.putExtra(EXTRA_NOTIFICATION_ALARM_TYPE, alarmType);
        intent.putExtra(EXTRA_NOTIFICATION_ALARM_ID, alarmID);
        intent.putExtra(EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY, alarmHourOfDay);
        intent.putExtra(EXTRA_NOTIFICATION_ALARM_MIN, alarmMin);
        intent.putExtra(EXTRA_NOTIFICATION_RINGTONE, ringTone);
        intent.putExtra(EXTRA_NOTIFICATION_VIBRATE, toVibrate);
        intent.putExtra(EXTRA_NOTIFICATION_REPEAT, repeatOption);
        intent.putExtra(EXTRA_NOTIFICATION_LABEL, label);
        intent.putExtra(EXTRA_NOTIFICATION_ICON_ID, iconID);
        if (action.equalsIgnoreCase(START_ALARM)) {
            intent.putExtra(EXTRA_NOTIFICATION_RESTART, true);
        }
        context.sendBroadcast(intent);
    }

    /**
     * Use this utility function to play a ringtone & vibrate.
     *
     * @param context       App Context
     * @param ringTonePath  Full path to Ringtone
     * @param toVibrate     true - Vibrate, false - Don't Vibrate
     *
     * @return true if Ringtone could be played, false otherwise.
     */
    private boolean playRingToneAndVibration(Context context, int alarmID, String ringTonePath,
                                             boolean toVibrate) {
        boolean isRingtoneAlreadyPlaying = false;
        Log.i("NPBroadcastReceiver", "Start playing ringtone!");
        Uri defUri = RingtoneManager.getActualDefaultRingtoneUri(context,
                RingtoneManager.TYPE_ALARM);

        // This could mean any of the following:
        // 1) No ringtone is playing --- Action: Start playing new ringtone.
        // 2) Ringtone is playing now for an alarm that fired --- Action: Snooze Alarm for 10 mins
        if (ringTonePlayer == null) {
            if (ringTonePath != null) {
                try {
                    if (ringTonePath.equalsIgnoreCase("Default")) {
                        defUri = RingtoneManager.getActualDefaultRingtoneUri(
                                context, RingtoneManager.TYPE_ALARM);
                    } else {
                        defUri = Uri.parse(ringTonePath);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("NPBroadcastReceiver", "RingTone Path: " + ringTonePath);
            }

            try {
                ringTonePlayer = RingtoneManager.getRingtone(context, defUri);
                ringTonePlayer.setStreamType(AudioManager.STREAM_ALARM);
                ringTonePlayer.play();
                alarmIDForPlayingRingtone = alarmID;
                Log.i("NPBroadcastReceiver", "Playing ringtone: " + defUri + " !");
            } catch (Exception e) {
                e.printStackTrace();
                // Falling back to default
                ringTonePlayer = RingtoneManager.getRingtone(context, defUri);
                ringTonePlayer.setStreamType(AudioManager.STREAM_ALARM);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringTonePlayer.setLooping(true);
                }
                ringTonePlayer.play();
                alarmIDForPlayingRingtone = alarmID;
                Log.i("NPBroadcastReceiver", "Playing Ringtone: " + defUri +
                        " for Alarm: " + alarmID + " !");
            }

            if (toVibrate) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    final long[] pattern = {2000, 1000, 2000, 1000};
                    vibrator.vibrate(pattern, 0);
                }
            }
        } else {
            isRingtoneAlreadyPlaying = true;
        }

        return isRingtoneAlreadyPlaying;
    }

    /**
     * Use this utility function to stop Ringtone & vibration.
     *
     * @param context           App Context
     */
    private void stopRingToneAndVibration(Context context, int alarmID) {
        // Stop Ringtone only for the Alarm that is running.
        // There may be other alarms that may get cancelled. It is no-op for those Alarms.
        if (alarmIDForPlayingRingtone == Alarm.INVALID_VALUE) {
            Log.d("NPBroadcastReceiver", "NO Ringtone playing. NOTHING to STOP!");
        } else if (alarmID == alarmIDForPlayingRingtone) {
            if (ringTonePlayer != null) {
                ringTonePlayer.stop();
                ringTonePlayer = null;
                alarmIDForPlayingRingtone = Alarm.INVALID_VALUE;
                Log.d("NPBroadcastReceiver", "RingTone STOPPED for Alarm: " + alarmID);
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.cancel();
                }
            }
        } else {
            Log.d("NPBroadcastReceiver", "RingTone NOT PLAYING for Alarm: " +
                    alarmID + ". Ringtone is playing for Alarm: " + alarmIDForPlayingRingtone);
        }
    }

    /**
     * Use this utility function to find the Dina Vishesham code(s) for the given calendar day.
     *
     * @param context           App Context
     *
     * @return List of Dina Vishesham code(s) for the given calendar day.
     */
    private List<Integer> findDinaVishesham(Context context) {
        List<Integer> dhinaSpecialCodeList = null;
        try {
            Calendar currCalendar = Calendar.getInstance();
            String location = MainActivity.readDefLocationSetting(context);

            MainActivity.PlacesInfo placesInfo = MainActivity.getLocationFromPlacesDB(location);

            String localpath = context.getFilesDir() + File.separator + "/ephe";
            VedicCalendar.initSwissEph(localpath);
            HashMap<String, String[]> vedicCalendarLocaleList =
                    MainActivity.buildVedicCalendarLocaleList(context);
            int ayanamsaMode = MainActivity.readPrefAyanamsaSelection(context);
            VedicCalendar vedicCalendar = VedicCalendar.getInstance(
                    MainActivity.readPrefPanchangamType(context), currCalendar, placesInfo.longitude,
                    placesInfo.latitude, placesInfo.timezone, ayanamsaMode,
                    MainActivity.readPrefChaandramanaType(context), vedicCalendarLocaleList);
            if (vedicCalendar != null) {
                vedicCalendar.getThithi(VedicCalendar.MATCH_SANKALPAM_EXACT);
                dhinaSpecialCodeList =
                        vedicCalendar.getDinaVishesham(VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
            }

            if (dhinaSpecialCodeList != null) {
                Log.d("NPBcastReceiver", "Dina Vishesham: " +
                        dhinaSpecialCodeList.toString() + " for " + location +
                        " Longitude: " + placesInfo.longitude + " Latitude: " + placesInfo.latitude);
            }
        } catch (Exception e) {
            // Create a dummy list!
            dhinaSpecialCodeList = new ArrayList<>();
        }

        return dhinaSpecialCodeList;
    }

    /**
     * Use this utility function to show a user-friendly notification text.
     *
     * @param alarmType     Alarm Type (Standard / Panchangam)
     * @param label         Alarm Label
     *
     * @return A String based on Alarm Type that can be used to show a user-friendly
     *         notification text.
     */
    private String getNotificationText(Context context, boolean alarmType, String label) {
        String notifText = "";
        if (label.isEmpty()) {
            notifText = context.getString(R.string.vedic);
        }

        if (alarmType == Alarm.ALARM_TYPE_VEDIC) {
            notifText += label + " " + context.getString(R.string.vedic_reminder_is_on);
        } else {
            notifText += label + " " + context.getString(R.string.vedic_alarm_is_on);
        }

        return notifText;
    }
}