package com.gkmhc.vedanta.nithya_panchangam;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Alarm Adapter to organize Alarm Information in ListView format.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class AlarmAdapter extends ArrayAdapter<Integer> {
    private final Fragment alarmFragment;
    private final Context context;
    private final ArrayList<Integer> alarmIDs;
    private final HashMap<Integer, NPDB.AlarmInfo> alarmsDB;

    public AlarmAdapter(@NonNull Context context, ArrayList<Integer> alarmIDList,
                        HashMap<Integer, NPDB.AlarmInfo> alarmsDB, Fragment fragment) {
        super(context, R.layout.alarm_row, R.id.alarm_date_time, alarmIDList);
        this.context = context;
        this.alarmIDs = alarmIDList;
        this.alarmsDB = alarmsDB;
        this.alarmFragment = fragment;
    }

    @Override
    public int getPosition(@Nullable Integer item) {
        //Log.d("AlarmAdapter","getPosition(" + item + ")!");
        return super.getPosition(item);
    }

    @Nullable
    @Override
    public Integer getItem(int position) {
        //Log.d("AlarmAdapter","getItem(" + position + ")!");
        return alarmIDs.get(position);
    }

    @Override
    public long getItemId(int position) {
        //Log.d("AlarmAdapter","getItemId(" + position + ")!");
        return position;
    }

    @Override
    public int getCount() {
        if (alarmIDs != null) {
            return alarmIDs.size();
        }
        return 0;
    }

    // Issue: If there are 10 Alarms, then onscreen only 5 are shown.
    //        When the screen is scrolled, then some viewholders are reused leading to duplicate (or)
    //        incorrect entries in the listview
    // Solution: By adding below override we are informing NOT to recycle the listview!
    // Side-Effect: Can this lead to performance issues since recycle is not done?
    //              For less than 50 alarms, can we live with the issue for now?
    // TODO - Need to come up with a permanent solution considering performance issues as well.
    @Override
    public int getViewTypeCount() {
        return getCount();
    }

    // Issue: If there are 10 Alarms, then onscreen only 5 are shown.
    //        When the screen is scrolled, then some viewholders are reused leading to duplicate (or)
    //        incorrect entries in the listview
    // Solution: By adding below override we are informing NOT to recycle the listview!
    // Side-Effect: Can this lead to performance issues since recycle is not done?
    //              For less than 50 alarms, can we live with the issue for now?
    // TODO - Need to come up with a permanent solution considering performance issues as well.
    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AlarmViewHolder alarmHolder;

        //Log.d("AlarmAdapter","getView(" + position + ")!");
        int alarmID = alarmIDs.get(position);
        NPDB.AlarmInfo alarmInfo = alarmsDB.get(alarmID);

        // Parse Alarm Value to retrieve Alarm ID information.
        // Key: {AlarmID}
        // Value: {AlarmType, AlarmStatus, AlarmHour, AlarmMinute, AlarmRingTone, AlarmVibrate,
        //         AlarmRepeat, AlarmLabel}
        if (alarmInfo != null) {
            boolean alarmType = alarmInfo.alarmType;
            boolean isAlarmOn = alarmInfo.isAlarmOn;
            int alarmHourOfDay = alarmInfo.alarmHourOfDay;
            int alarmMin = alarmInfo.alarmMin;
            String ringTone = alarmInfo.ringTone;
            boolean toVibrate = alarmInfo.toVibrate;
            int repeatOption = alarmInfo.repeatOption;
            String label = alarmInfo.label;

            // Create Item only once.
            // This function can be called multiple times for the same Item.
            if (convertView == null) {
                convertView = View.inflate(context, R.layout.alarm_row, null);
                alarmHolder = new AlarmViewHolder(convertView);
                convertView.setTag(alarmHolder);

                alarmHolder.alarmState.setChecked(isAlarmOn == Alarm.ALARM_STATE_ON);
                if (isAlarmOn == Alarm.ALARM_STATE_ON) {
                    Log.i("AlarmAdapter", "Triggering Alarm(" + alarmID + ")!");
                    Alarm.startAlarm(context, alarmType, alarmID, alarmHourOfDay, alarmMin, ringTone,
                            toVibrate, repeatOption, label, Alarm.DEF_ICON_ID);
                }
            } else {
                alarmHolder = (AlarmViewHolder) convertView.getTag();
            }

            convertView.setId(alarmID);
            convertView.setClickable(true);
            convertView.setFocusable(true);
            convertView.setLongClickable(true);
            alarmHolder.alarmDateTime.setText(String.format("%02d:%02d", alarmHourOfDay, alarmMin));
            alarmHolder.alarmLabel.setText(label);
            alarmHolder.alarmAddlSettings.setText(Alarm.getRepeatOptionText(context, repeatOption));

            // When Alarm is toggled, trigger start or stop the alarm.
            alarmHolder.alarmState.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    Log.i("AlarmAdapter", "Start Alarm(" + alarmID + " position: " +
                            position + ") Alarm Time: " + alarmHourOfDay + ":" + alarmMin);
                    alarmInfo.isAlarmOn = Alarm.ALARM_STATE_ON;
                    alarmsDB.put(alarmID, alarmInfo);
                    Alarm.startAlarm(context, alarmType, alarmID, alarmHourOfDay, alarmMin, ringTone,
                            toVibrate, repeatOption, label, Alarm.DEF_ICON_ID);
                } else {
                    Log.i("AlarmAdapter", "Stop Alarm(" + alarmID + " position: " +
                            position + ") Alarm Time: " + alarmHourOfDay + ":" + alarmMin);
                    alarmInfo.isAlarmOn = Alarm.ALARM_STATE_OFF;
                    alarmsDB.put(alarmID, alarmInfo);
                    Alarm.stopAlarm(context, alarmType, alarmID, alarmHourOfDay, alarmMin, ringTone,
                                    toVibrate, repeatOption, label, Alarm.DEF_ICON_ID, false);
                }
            });

            // When Alarm item is clicked, then load same Add Alarm Activity but to modify!
            convertView.setOnClickListener(view -> {
                Intent intent = new Intent(view.getContext(), HandleAlarmReminderActivity.class);
                intent.putExtra(Alarm.EXTRA_ALARM_ALARM_ID, alarmID);
                intent.putExtra(Alarm.EXTRA_ALARM_ALARM_HOUR_OF_DAY, alarmHourOfDay);
                intent.putExtra(Alarm.EXTRA_ALARM_ALARM_MIN, alarmMin);
                intent.putExtra(Alarm.EXTRA_ALARM_TYPE, alarmType);
                intent.putExtra(Alarm.EXTRA_ALARM_VIBRATE, toVibrate);
                intent.putExtra(Alarm.EXTRA_ALARM_REPEAT, repeatOption);
                intent.putExtra(Alarm.EXTRA_ALARM_RINGTONE, ringTone);
                intent.putExtra(Alarm.EXTRA_ALARM_LABEL, label);
                alarmFragment.startActivityForResult(intent, Alarm.ALARM_REQUEST_CODE);
            /*Log.d("AlarmAdapter","onItemClick(Position: " + position + " AlarmID: " +
                    alarmID + ")!");*/
            });
        }

        return convertView;
    }
}