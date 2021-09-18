package com.gkmhc.vedanta.nithya_panchangam;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Alarm fragment to handle list of Alarms.
 * Handle {Create, Delete, Start, Stop, Restart} Alarm functions.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class Alarm extends Fragment {
    private View root;
    private ListView alarmListView;
    private Context context;
    private int delAlarmPos = INVALID_VALUE;
    private View selectedAlarmItem;
    private FloatingActionButton fabAdd;
    private FloatingActionButton fabDel;
    public static final int DEF_ICON_ID = R.drawable.swamy_ayyappan_circle;
    public static final int INVALID_VALUE = -1;
    public static final int ALARM_REQUEST_CODE = 3456;
    public static final int ALARM_TYPE_STANDARD_ID_START = 500;
    public static final boolean ALARM_TYPE_STANDARD = false;
    public static final boolean ALARM_TYPE_VEDIC = true;
    public static final boolean ALARM_STATE_ON = true;
    public static final boolean ALARM_STATE_OFF = false;

    // Repeat options ranges from 0 to 9
    public static final int ALARM_REPEAT_SUN = 0;
    public static final int ALARM_REPEAT_MON = 1;
    public static final int ALARM_REPEAT_TUE = 2;
    public static final int ALARM_REPEAT_WED = 3;
    public static final int ALARM_REPEAT_THU = 4;
    public static final int ALARM_REPEAT_FRI = 5;
    public static final int ALARM_REPEAT_SAT = 6;
    public static final int ALARM_REPEAT_ONCE = 7;
    public static final int ALARM_REPEAT_DAILY = 8;
    public static final int ALARM_REPEAT_EVERY_OCCURRENCE = 9;
    public static final String EXTRA_ALARM_ALARM_ID = "Alarm_Extra_AlarmID";
    public static final String EXTRA_ALARM_ALARM_HOUR_OF_DAY = "Alarm_Extra_AlarmHourOfDay";
    public static final String EXTRA_ALARM_ALARM_MIN = "Alarm_Extra_AlarmMin";
    public static final String EXTRA_ALARM_RINGTONE = "Alarm_Extra_Ringtone_Settings";
    public static final String EXTRA_ALARM_VIBRATE = "Alarm_Extra_Vibrate";
    public static final String EXTRA_ALARM_REPEAT = "Alarm_Extra_Repeat";
    public static final String EXTRA_ALARM_LABEL = "Alarm_Extra_Label";
    public static final String EXTRA_ALARM_TYPE = "Alarm_Extra_Alarm_Type";
    public static final String EXTRA_ALARM_ICON_ID = "Alarm_Extra_Alarm_Icon_ID";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getContext();
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.updateAppLocale();
        }

        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_alarm, container, false);
        alarmListView = root.findViewById(R.id.alarm_table);

        // Add a new Alarm
        fabAdd = root.findViewById(R.id.btn_add_alarm);
        fabAdd.setOnClickListener(v ->
                startActivityForResult(new Intent(getActivity(),
                        HandleAlarmReminderActivity.class), ALARM_REQUEST_CODE));

        // Delete an existing alarm
        fabDel = root.findViewById(R.id.btn_delete_alarm);
        fabDel.setVisibility(View.GONE);
        fabDel.setOnClickListener(view -> {
            HashMap<Integer, NPDB.AlarmInfo> alarmsDB = NPDB.readAlarmsFromDB(
                    context.getApplicationContext(),
                    Alarm.ALARM_TYPE_STANDARD);
            ArrayList<Integer> alarmsList = NPDB.getAlarmIDs(alarmsDB);

            // TODO: Is there a way to find out all the selected rows in a ListView?
            // Note: I did not find any easy way. Hence, the below approach to remember the selected
            // row & delete it here!
            if (alarmsList != null) {
                int alarmPosToDel = delAlarmPos;
                int alarmID = alarmsList.get(alarmPosToDel);

                NPDB.AlarmInfo alarmInfo = alarmsDB.get(alarmID);
                if (alarmInfo != null) {
                    int alarmHourOfDay = alarmInfo.alarmHourOfDay;
                    int alarmMin = alarmInfo.alarmMin;
                    String ringTone = alarmInfo.ringTone;
                    boolean toVibrate = alarmInfo.toVibrate;
                    boolean alarmType = alarmInfo.alarmType;
                    int repeatOption = alarmInfo.repeatOption;
                    String label = alarmInfo.label;

                    // Notify Broadcast Receive to delete the Alarm!
                    NPBroadcastReceiver.notifyBroadcastReceiver(context,
                            NPBroadcastReceiver.DELETE_ALARM, alarmType, alarmID, alarmHourOfDay, alarmMin,
                            ringTone, toVibrate, repeatOption, label, Alarm.DEF_ICON_ID);

                    Log.i("AlarmAdapter", "Deleting Alarm(" + alarmID + ")!");

                    alarmsList.remove(alarmPosToDel);
                    NPDB.removeAlarmFromDB(context, alarmType, alarmID);

                    delAlarmPos = INVALID_VALUE;
                    selectedAlarmItem = null;

                    fabDel.setVisibility(View.GONE);
                    fabAdd.setVisibility(View.VISIBLE);
                    updateAlarmsListView();
                }
            }
        });

        refreshAlarms();
        return root;
    }

    public void refreshAlarms() {
        new Thread() {
            @Override
            public void run() {
                try {
                    // code runs in a thread
                    getActivity().runOnUiThread(() -> updateAlarmsListView());
                } catch (final Exception ex) {
                    Log.d("Alarm","Exception in initAlarms()");
                }
            }
        }.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Log.i("Alarm","Request Code: " + requestCode + " Response Code: " + resultCode);

        // Upon Activity Results, do the following:
        // 1) Create new Alarm based on information provided (not yet supported)
        // 2) Update Alarm with the information provided by the user
        if (requestCode == ALARM_REQUEST_CODE) {
            if (data != null) {
                int alarmID = data.getIntExtra(EXTRA_ALARM_ALARM_ID, INVALID_VALUE);
                int alarmHourOfDay = data.getIntExtra(EXTRA_ALARM_ALARM_HOUR_OF_DAY, 0);
                int alarmMin = data.getIntExtra(EXTRA_ALARM_ALARM_MIN, 0);
                boolean alarmType = data.getBooleanExtra(EXTRA_ALARM_TYPE, Alarm.ALARM_TYPE_STANDARD);
                boolean toVibrate = data.getBooleanExtra(EXTRA_ALARM_VIBRATE, false);
                int repeatOption = data.getIntExtra(EXTRA_ALARM_REPEAT, ALARM_REPEAT_ONCE);
                String ringTone = data.getStringExtra(EXTRA_ALARM_RINGTONE);
                String label = data.getStringExtra(EXTRA_ALARM_LABEL);
                Log.i("Alarm", "Alarm Code: " + " Alarm Time: " + alarmHourOfDay + ":" + alarmMin);

                if (alarmType == Alarm.ALARM_TYPE_STANDARD) {
                    // Scenario: Modify existing alarm
                    if (alarmID != INVALID_VALUE) {
                        /*restartAlarm(context, alarmType, alarmID, alarmHourOfDay, alarmMin,
                                     ringTone, toVibrate, repeatOption, label, DEF_ICON_ID);*/
                        addStandardAlarm(context, alarmType, alarmID, alarmHourOfDay,
                                         alarmMin, ringTone, toVibrate, repeatOption, label);
                    } else {
                        // Scenario: Create a new alarm
                        int newAlarmID = generateNewAlarmID(context);
                        if (newAlarmID != INVALID_VALUE) {
                            addStandardAlarm(context, alarmType, newAlarmID,
                                    alarmHourOfDay, alarmMin, ringTone, toVibrate,
                                             repeatOption, label);
                        } else {
                            Log.i("Alarm",
                                    "ERROR: No Alarm IDs available! Cannot create new Alarm!");
                        }
                    }
                    updateAlarmsListView();
                }
            }
        }
    }

    /**
     * Use this API to generate a new Alarm from the list of available Alarms.
     * Alarm Range starts from 500 to Integer Max.
     *
     * @param context   Application Context
     *
     * @return Return a unique Alarm ID
     */
    public static int generateNewAlarmID(Context context) {
        HashMap<Integer, NPDB.AlarmInfo> alarmsDB = NPDB.readAlarmsFromDB(
                context.getApplicationContext(), Alarm.ALARM_TYPE_STANDARD);

        // Get the first available Alarm ID
        // Range is from 0 to 2147483647.
        // Don't think we need to iterate so much.
        // We should be getting a valid Alarm ID much before the max value.
        for (int alarmIter = ALARM_TYPE_STANDARD_ID_START; alarmIter < Integer.MAX_VALUE;
             alarmIter++) {
            if (alarmsDB.get(alarmIter) == null) {
                Log.i("Alarm", "Found New Alarm ID: " + alarmIter + " AVAILABLE!");
                return alarmIter;
            } else {
                Log.i("Alarm", "Alarm ID: " + alarmIter + " already in USE!");
            }
        }
        return INVALID_VALUE;
    }

    /**
     * Use this API to add or update standard Alarm.
     * @param context           App Context
     * @param alarmType         Alarm Type (Standard / Panchangam)
     * @param alarmID           Alarm ID
     * @param alarmHourOfDay    Alarm Hour of Day (24-hour format)
     * @param alarmMin          Alarm Minutes
     * @param ringTone          Full path to Ringtone
     * @param toVibrate         True - Vibrate, False - Do NOT Vibrate
     * @param repeatOption      Once   - Buzz Alarm one-time
     *                          Daily  - Buzz Alarm daily at the Alarm Hour & Minute
     *                          Custom - Buzz Alarm at the Alarm Hour & Minute on selected days
     * @param label             Alarm Label
     */
    private static void addStandardAlarm(Context context, boolean alarmType, int alarmID,
                                         int alarmHourOfDay, int alarmMin, String ringTone,
                                         boolean toVibrate, int repeatOption, String label) {
        // Start Alarm by default
        if (NPDB.isAlarmInDB(context, alarmType, alarmID)) {
            NPDB.updateAlarmInfoInDB(context, alarmType, alarmID, Alarm.ALARM_STATE_ON,
                    alarmHourOfDay, alarmMin, ringTone, toVibrate, repeatOption, label);

            // Three scenarios in which this can be called:
            // 1) Start New Alarm
            // 2) Modify existing Alarm
            // 3) When App starts, if Alarm state is "On", then recreate Alarm
            if (Alarm.ALARM_STATE_ON == ALARM_STATE_ON) {
                restartAlarm(context, alarmType, alarmID, alarmHourOfDay, alarmMin, ringTone,
                        toVibrate, repeatOption, label, Alarm.DEF_ICON_ID);
            }
        } else {
            NPDB.addAlarmToDB(context, alarmType, alarmID, Alarm.ALARM_STATE_ON, alarmHourOfDay, alarmMin,
                              ringTone, toVibrate, repeatOption, label);
        }
    }

    /**
     * Use this API to update the listview in Alarm fragment with the retrieved list of Alarms.
     */
    private void updateAlarmsListView() {
        TextView noAlarmsView = root.findViewById(R.id.no_alarms);
        noAlarmsView.setText(R.string.no_alarms);

        HashMap<Integer, NPDB.AlarmInfo> alarmsDB = NPDB.readAlarmsFromDB(
                context.getApplicationContext(), Alarm.ALARM_TYPE_STANDARD);
        ArrayList<Integer> alarmsList = NPDB.getAlarmIDs(alarmsDB);
        if (alarmsList != null) {
            if (alarmsList.size() == 0) {
                noAlarmsView.setVisibility(View.VISIBLE);
                alarmListView.setVisibility(View.GONE);
            } else {
                // Show listview only if there is atleast 1 Alarm to display.
                noAlarmsView.setVisibility(View.GONE);
                alarmListView.setVisibility(View.VISIBLE);

                AlarmAdapter alarmAdapter = new AlarmAdapter(context, alarmsList, alarmsDB, this);
                alarmListView.setAdapter(alarmAdapter);

                alarmListView.setOnItemLongClickListener((parent, view, position, itemID) -> {
                    Log.d("Alarm", "onItemLongClick(" + position + ")! ID: " + itemID);
                    if (delAlarmPos != INVALID_VALUE) {
                        if (itemID == delAlarmPos) {
                            // Deselect already selected Alarm row
                            view.setBackgroundColor(Color.TRANSPARENT);
                            view.setSelected(false);
                            Log.d("Alarm", "OnLongClick(" + itemID + ") UNSELECTED!");
                            fabAdd.setVisibility(View.VISIBLE);
                            fabDel.setVisibility(View.GONE);
                            delAlarmPos = INVALID_VALUE;
                            selectedAlarmItem = null;
                        } else {
                            // Select a new Alarm row when some other Alarm row is selected
                            view.setBackgroundColor(Color.WHITE);
                            view.setSelected(true);
                            Log.d("Alarm", "OnLongClick(" + itemID + ") Another SELECTED!");
                            selectedAlarmItem.setBackgroundColor(Color.TRANSPARENT);
                            selectedAlarmItem.setSelected(false);
                            delAlarmPos = (int) itemID;
                            selectedAlarmItem = view;
                        }
                    } else {
                        // Select a new Alarm row when NO other Alarm row is selected
                        view.setBackgroundColor(Color.WHITE);
                        view.setSelected(true);
                        Log.d("Alarm", "OnLongClick(" + itemID + ") SELECTED!");
                        fabAdd.setVisibility(View.GONE);
                        fabDel.setVisibility(View.VISIBLE);
                        delAlarmPos = (int) itemID;
                        selectedAlarmItem = view;
                    }
                    return true;
                });
            }
        } else {
            noAlarmsView.setVisibility(View.VISIBLE);
            alarmListView.setVisibility(View.GONE);
        }
    }

    /**
     * Use this utility function to handle "back" button press.
     * Typically useful to deselect a selected Alarm row item.
     *
     * @return True if event processed, false otherwise.
     */
    public boolean handleBackPressedEvent() {
        if (delAlarmPos != INVALID_VALUE) {
            selectedAlarmItem.setBackgroundColor(Color.TRANSPARENT);
            selectedAlarmItem.setSelected(false);
            Log.d("Alarm", "onBackPressed(" + delAlarmPos + ") UNSELECTED!");
            fabAdd.setVisibility(View.VISIBLE);
            fabDel.setVisibility(View.GONE);
            delAlarmPos = INVALID_VALUE;
            selectedAlarmItem = null;
            return true;
        }

        return false;
    }

    /**
     * Use this API to inform NPBroadcastReceiver to start the given Alarm.
     *
     * @param context           App Context
     * @param alarmType         Alarm Type (Standard / Panchangam)
     * @param alarmID           Alarm ID
     * @param alarmHourOfDay    Alarm Hour of Day (24-hour format)
     * @param alarmMin          Alarm Minutes
     * @param ringTone          Full path to Ringtone
     * @param toVibrate         True - Vibrate, False - Do NOT Vibrate
     * @param repeatOption      Once   - Buzz Alarm one-time
     *                          Daily  - Buzz Alarm daily at the Alarm Hour & Minute
     *                          Custom - Buzz Alarm at the Alarm Hour & Minute on selected days
     * @param label             Alarm Label
     */
    public static void startAlarm(Context context, boolean alarmType, int alarmID,
                                  int alarmHourOfDay, int alarmMin, String ringTone,
                                  boolean toVibrate, int repeatOption, String label, int iconID) {
        try {
            if (!isAlarmRunning(context, alarmID)) {
                Intent intent = new Intent(context.getApplicationContext(), NPBroadcastReceiver.class);
                intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_TYPE, alarmType);
                intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_ID, alarmID);
                intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY, alarmHourOfDay);
                intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_MIN, alarmMin);
                intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_RINGTONE, ringTone);
                intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_VIBRATE, toVibrate);
                intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_REPEAT, repeatOption);
                intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_LABEL, label);
                intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_ICON_ID, iconID);
                intent.setAction(NPBroadcastReceiver.START_ALARM);
                context.sendBroadcast(intent);
                Log.i("Alarm","Scheduling Alarm(" + alarmID + ") with Ringtone: " + ringTone + " !");
            } else {
                Log.i("Alarm","Alarm(" + alarmID + ") already RUNNING!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Use this API to inform NPBroadcastReceiver to restart the given Alarm.
     *
     * @param context           App Context
     * @param alarmType         Alarm Type (Standard / Panchangam)
     * @param alarmID           Alarm ID
     * @param alarmHourOfDay    Alarm Hour of Day (24-hour format)
     * @param alarmMin          Alarm Minutes
     * @param ringTone          Full path to Ringtone
     * @param toVibrate         True - Vibrate, False - Do NOT Vibrate
     * @param repeatOption      Once   - Buzz Alarm one-time
     *                          Daily  - Buzz Alarm daily at the Alarm Hour & Minute
     *                          Custom - Buzz Alarm at the Alarm Hour & Minute on selected days
     * @param label             Alarm Label
     */
    public static void restartAlarm(Context context, boolean alarmType, int alarmID,
                                    int alarmHourOfDay, int alarmMin, String ringTone,
                                    boolean toVibrate, int repeatOption, String label,
                                    int iconID) {
        try {
            Intent intent = new Intent(context.getApplicationContext(), NPBroadcastReceiver.class);
            intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_TYPE, alarmType);
            intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_ID, alarmID);
            intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_HOUR_OF_DAY, alarmHourOfDay);
            intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_MIN, alarmMin);
            intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_RINGTONE, ringTone);
            intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_VIBRATE, toVibrate);
            intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_REPEAT, repeatOption);
            intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_LABEL, label);
            intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_ICON_ID, iconID);
            intent.setAction(NPBroadcastReceiver.START_ALARM);
            if (isAlarmRunning(context, alarmID)) {
                intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_RESTART, true);
                Log.i("Alarm","ReScheduling Alarm(" + alarmID + ") with Ringtone: " + ringTone + " !");
            } else {
                Log.i("Alarm","Alarm(" + alarmID + ") NOT RUNNING, so creating new Alarm!");
            }
            context.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Use this API to inform NPBroadcastReceiver to stop a given Alarm.
     *
     * @param context           App Context
     * @param alarmType         Alarm Type (Standard / Panchangam)
     * @param alarmID           Alarm ID
     * @param alarmHourOfDay    Alarm Hour of Day (24-hour format)
     * @param alarmMin          Alarm Minutes
     * @param ringTone          Full path to Ringtone
     * @param toVibrate         True - Vibrate, False - Do NOT Vibrate
     * @param repeatOption      Once   - Buzz Alarm one-time
     *                          Daily  - Buzz Alarm daily at the Alarm Hour & Minute
     *                          Custom - Buzz Alarm at the Alarm Hour & Minute on selected days
     * @param label             Alarm Label
     */
    public static void stopAlarm(Context context, boolean alarmType, int alarmID,
                                 int alarmHourOfDay, int alarmMin, String ringTone,
                                 boolean toVibrate, int repeatOption, String label, int iconID,
                                 boolean toDelete) {
        try {
            if (toDelete) {
                NPBroadcastReceiver.notifyBroadcastReceiver(context,
                        NPBroadcastReceiver.DELETE_ALARM, alarmType, alarmID,
                        alarmHourOfDay, alarmMin, ringTone, toVibrate, repeatOption, label, iconID);
                NPDB.removeAlarmFromDB(context, alarmType, alarmID);
            } else {
                NPBroadcastReceiver.notifyBroadcastReceiver(context,
                        NPBroadcastReceiver.STOP_ALARM, alarmType, alarmID,
                        alarmHourOfDay, alarmMin, ringTone, toVibrate, repeatOption, label, iconID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Use this API to check if the given Alarm is running or not.
     *
     * @param context           App Context
     * @param alarmID           Alarm ID
     *
     * @return True if running, false otherwise.
     */
    public static boolean isAlarmRunning(Context context, int alarmID) {
        Intent intent = new Intent(context, NPBroadcastReceiver.class);
        intent.putExtra(NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_ID, alarmID);
        intent.setAction(NPBroadcastReceiver.START_ALARM_HANDLER);
        return ((PendingIntent.getBroadcast(context,
                alarmID, intent, PendingIntent.FLAG_NO_CREATE)) != null);
    }

    public static String getRepeatOptionText(Context context, int repeatOption) {
        StringBuilder repeatOptionText = new StringBuilder();

        switch (repeatOption) {
            case ALARM_REPEAT_ONCE:
                repeatOptionText = new StringBuilder(context.getString(R.string.repeat_once));
                break;
            case ALARM_REPEAT_DAILY:
                repeatOptionText = new StringBuilder(context.getString(R.string.repeat_daily));
                break;
            case ALARM_REPEAT_EVERY_OCCURRENCE:
                repeatOptionText = new StringBuilder(context.getString(R.string.repeat_every_occurrence));
                break;
            default:
                String repeatOptionStr = String.valueOf(repeatOption);
                int len = repeatOptionStr.length();
                int startIndex = 0;
                while (startIndex < len) {
                    int repeatCustomDay =
                            Character.getNumericValue(repeatOptionStr.charAt(startIndex++));
                    if (repeatCustomDay == Alarm.ALARM_REPEAT_SUN) {
                        repeatOptionText.append(context.getString(R.string.planet_sun));
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_MON) {
                        repeatOptionText.append(context.getString(R.string.planet_mon));
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_TUE) {
                        repeatOptionText.append(context.getString(R.string.planet_tue));
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_WED) {
                        repeatOptionText.append(context.getString(R.string.planet_wed));
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_THU) {
                        repeatOptionText.append(context.getString(R.string.planet_thu));
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_FRI) {
                        repeatOptionText.append(context.getString(R.string.planet_fri));
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_SAT) {
                        repeatOptionText.append(context.getString(R.string.planet_sat));
                    }
                    repeatOptionText.append(" ");
                }
                break;
        }

        return repeatOptionText.toString();
    }
}