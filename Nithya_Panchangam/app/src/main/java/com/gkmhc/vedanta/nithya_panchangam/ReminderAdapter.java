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
public class ReminderAdapter extends ArrayAdapter<Integer> {
    private final Fragment reminderFragment;
    private final Context context;
    private final ArrayList<Integer> reminderAlarmIDList;
    private final HashMap<Integer, NPDB.AlarmInfo> remindersDB;

    public ReminderAdapter(@NonNull Context context, ArrayList<Integer> reminderAlarmIDList,
                           HashMap<Integer, NPDB.AlarmInfo> remindersDB,
                           Fragment reminderFragment) {
        super(context, R.layout.reminder_row, R.id.reminder_icon, reminderAlarmIDList);
        this.context = context;
        this.reminderAlarmIDList = reminderAlarmIDList;
        this.remindersDB = remindersDB;
        this.reminderFragment = reminderFragment;
    }

    @Override
    public int getPosition(@Nullable Integer item) {
        //Log.d("ReminderAdapter","getPosition(" + item + ")!");
        return super.getPosition(item);
    }

    @Nullable
    @Override
    public Integer getItem(int position) {
        //Log.d("ReminderAdapter","getItem(" + position + ")!");
        return reminderAlarmIDList.get(position);
    }

    @Override
    public long getItemId(int position) {
        //Log.d("ReminderAdapter","getItemId(" + position + ")!");
        return position;
    }

    @Override
    public int getCount() {
        return reminderAlarmIDList.size();
    }

    // Issue: If there are 10 Reminders, then onscreen only 5 are shown.
    //        When the screen is scrolled, then some viewholders are reused leading to duplicate (or)
    //        incorrect entries in the listview
    // Solution: By adding below override we are informing NOT to recycle the listview!
    // Side-Effect: Can this lead to performance issues since recycle is not done?
    //              For less than 50 reminders, can we live with the issue for now?
    // TODO - Need to come up with a permanent solution considering performance issues as well.
    @Override
    public int getViewTypeCount() {
        return getCount();
    }

    // Issue: If there are 10 Reminders, then onscreen only 5 are shown.
    //        When the screen is scrolled, then some viewholders are reused leading to duplicate (or)
    //        incorrect entries in the listview
    // Solution: By adding below override we are informing NOT to recycle the listview!
    // Side-Effect: Can this lead to performance issues since recycle is not done?
    //              For less than 50 reminders, can we live with the issue for now?
    // TODO - Need to come up with a permanent solution considering performance issues as well.
    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ReminderViewHolder reminderHolder;
        int reminderID = reminderAlarmIDList.get(position);
        NPDB.AlarmInfo reminderInfo = remindersDB.get(reminderID);

        // Parse Reminder Info to retrieve Reminder ID information.
        // Key: {ReminderID}
        // Value: {AlarmType, AlarmStatus, AlarmHour, AlarmMinute, AlarmRingTone, AlarmVibrate,
        //         AlarmRepeat, AlarmLabel, iconID}
        // All above Value fields delimited by "---"
        if (reminderInfo != null) {
            boolean reminderType = reminderInfo.alarmType;
            int reminderHourOfDay = reminderInfo.alarmHourOfDay;
            int reminderMin = reminderInfo.alarmMin;
            int repeatOption = reminderInfo.repeatOption;
            String ringTone = reminderInfo.ringTone;
            boolean isReminderOn = reminderInfo.isAlarmOn;
            String label = reminderInfo.label;
            boolean toVibrate = reminderInfo.toVibrate;
            //int iconID = reminderInfo.iconID;
            int iconID = Reminder.getDinaVisheshamImg(reminderID);

            //Log.d("ReminderAdapter","getView(" + position + ")!");
            if (convertView == null) {
                convertView = View.inflate(context, R.layout.reminder_row, null);
                reminderHolder = new ReminderViewHolder(convertView);
                convertView.setTag(reminderHolder);
                //Log.d("ReminderAdapter","Reminder(" + reminderID + ") state: " + reminderState + " !");
                reminderHolder.reminderState.setChecked(isReminderOn == Alarm.ALARM_STATE_ON);
                if (isReminderOn == Alarm.ALARM_STATE_ON) {
                    Log.i("ReminderAdapter", "Triggering Reminder(" + reminderID + ")!");
                    Alarm.startAlarm(context, reminderType, reminderID, reminderHourOfDay, reminderMin, ringTone,
                            toVibrate, repeatOption, label, iconID);
                }
            } else {
                reminderHolder = (ReminderViewHolder) convertView.getTag();
            }

            convertView.setId(reminderAlarmIDList.get(position));
            convertView.setClickable(true);
            convertView.setFocusable(true);
            convertView.setLongClickable(true);
            reminderHolder.reminderIcon.setImageResource(iconID);

            // Why is this done?
            // Label is stored in Reminder DB in one locale.
            // When locale is changed, we have two choices:
            // 1) Change locale for each Reminder in Reminder DB
            //    Pros: Clean, scalable
            //    Cons: Complex, time-consuming, visible delays seen everytime locale is changed
            // 2) Change only display and disregard locale stored in Reminder DB
            //    Pros: Simple, Scalable
            //    Cons: May not be extensible. Then why store label in DB? Redundant?
            int resID = Reminder.getDinaVisheshamLabel(reminderID);
            if (resID != -1) {
                label = reminderFragment.getString(resID);
            }
            String finalLabel = label;
            reminderHolder.reminderLabel.setText(label);
            reminderHolder.reminderDateTime.setText(String.format("%02d:%02d", reminderHourOfDay, reminderMin));
            reminderHolder.reminderAddlSettings.setText(Alarm.getRepeatOptionText(context, repeatOption));

            reminderHolder.reminderState.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    Log.i("ReminderAdapter", "Start Reminder(" + reminderID + " position: " +
                            position + ") Reminder Time: " + reminderHourOfDay + ":" + reminderMin);
                    reminderInfo.isAlarmOn = Alarm.ALARM_STATE_ON;
                    remindersDB.put(reminderID, reminderInfo);
                    Alarm.startAlarm(context, reminderType, reminderID, reminderHourOfDay, reminderMin, ringTone,
                            toVibrate, repeatOption, finalLabel, iconID);
                } else {
                    Log.i("ReminderAdapter", "Stop Reminder(" + reminderID + " position: " +
                            position + ") Reminder Time: " + reminderHourOfDay + ":" + reminderMin);
                    reminderInfo.isAlarmOn = Alarm.ALARM_STATE_OFF;
                    remindersDB.put(reminderID, reminderInfo);
                    Alarm.stopAlarm(context, reminderType, reminderID, reminderHourOfDay, reminderMin, ringTone,
                            toVibrate, repeatOption, finalLabel, iconID, false);
                }
            });

            // When Alarm item is clicked, then load same Add Alarm Activity but to modify Reminder!
            // AlarmType shall decide what options to display to the user.
            convertView.setOnClickListener(view -> {
                Intent intent = new Intent(view.getContext(), HandleAlarmReminderActivity.class);
                intent.putExtra(Alarm.EXTRA_ALARM_ALARM_ID, reminderID);
                intent.putExtra(Alarm.EXTRA_ALARM_ALARM_HOUR_OF_DAY, reminderHourOfDay);
                intent.putExtra(Alarm.EXTRA_ALARM_ALARM_MIN, reminderMin);
                intent.putExtra(Alarm.EXTRA_ALARM_VIBRATE, toVibrate);
                intent.putExtra(Alarm.EXTRA_ALARM_REPEAT, repeatOption);
                intent.putExtra(Alarm.EXTRA_ALARM_RINGTONE, ringTone);
                intent.putExtra(Alarm.EXTRA_ALARM_LABEL, finalLabel);
                intent.putExtra(Alarm.EXTRA_ALARM_TYPE, reminderType);
                intent.putExtra(Alarm.EXTRA_ALARM_ICON_ID, iconID);
                reminderFragment.startActivityForResult(intent, Reminder.REMINDER_REQUEST_CODE);
            /*Log.d("ReminderAdapter","onItemClick(Position: " + position +
                    " ReminderID: " + reminderID + ")!");*/
            });
        }

        return convertView;
    }
}