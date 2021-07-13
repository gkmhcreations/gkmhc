package com.gkmhc.vedanta.nithya_panchangam;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;

/**
 * Class to hold Reminder View Information in a row in a Reminder Listview
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class ReminderViewHolder {
    public final ImageView reminderIcon;
    public final TextView reminderDateTime;
    public final TextView reminderLabel;
    public final TextView reminderAddlSettings;
    public final SwitchCompat reminderState;

    ReminderViewHolder(View view) {
        reminderIcon = view.findViewById(R.id.reminder_icon);
        reminderDateTime = view.findViewById(R.id.reminder_date_time);
        reminderLabel = view.findViewById(R.id.reminder_label);
        reminderAddlSettings = view.findViewById(R.id.reminder_addl_settings);
        reminderState = view.findViewById(R.id.reminder_state);
    }
}
