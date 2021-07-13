package com.gkmhc.vedanta.nithya_panchangam;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Use this Calendar View Holder to hold the views related to the calendar dates.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class CalenderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public final TextView gregDaysOfMonth;
    public final TextView drikDaysOfMonth;
    public final ViewFlipper viewFlipper;
    public final ImageView drikCalendarImg1View;
    public final ImageView drikCalendarImg2View;
    public final ImageView drikCalendarImg3View;
    public final ImageView drikCalendarImg4View;
    private final CalendarAdapter.OnItemListener onItemListener;

    public CalenderViewHolder(@NonNull View itemView,
                              CalendarAdapter.OnItemListener onItemListener) {
        super(itemView);
        gregDaysOfMonth = itemView.findViewById(R.id.gregCalendarDay);
        drikDaysOfMonth = itemView.findViewById(R.id.drikCalendarDay);
        drikCalendarImg1View = itemView.findViewById(R.id.drikCalendarImg1);
        drikCalendarImg2View = itemView.findViewById(R.id.drikCalendarImg2);
        drikCalendarImg3View = itemView.findViewById(R.id.drikCalendarImg3);
        drikCalendarImg4View = itemView.findViewById(R.id.drikCalendarImg4);
        viewFlipper = itemView.findViewById(R.id.drikCalendarFlipImg);
        this.onItemListener = onItemListener;
        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        onItemListener.onItemClick(view, getAdapterPosition());
    }
}
