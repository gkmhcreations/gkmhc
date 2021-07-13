package com.gkmhc.vedanta.nithya_panchangam;

import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;

/**
 * Class to hold Alarm View Information in a row in a Alarm Listview.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class AlarmViewHolder {
    public final TextView alarmDateTime;
    public final TextView alarmLabel;
    public final TextView alarmAddlSettings;
    public final SwitchCompat alarmState;

    AlarmViewHolder(View view) {
        alarmDateTime = view.findViewById(R.id.alarm_date_time);
        alarmLabel = view.findViewById(R.id.alarm_label);
        alarmAddlSettings = view.findViewById(R.id.alarm_addl_settings);
        alarmState = view.findViewById(R.id.alarm_state);
    }
}
