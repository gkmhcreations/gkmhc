package com.gkmhc.vedanta.nithya_panchangam;

import android.view.View;
import android.widget.TextView;

/**
 * Class to hold Panchangam View Information in a row in a Panchangam Listview.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class PanchangamViewHolder {
    public final TextView panchangamField;
    public final TextView panchangamValue;

    PanchangamViewHolder(View view) {
        panchangamField = view.findViewById(R.id.panchangam_field);
        panchangamValue = view.findViewById(R.id.panchangam_value);
    }
}
