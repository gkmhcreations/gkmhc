package com.gkmhc.vedanta.nithya_panchangam;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Use this Calendar View Adapter to hold {date, dhinaankham, thithi, amruthathi yogam, icon}
 * related to the calendar dates in a given Gregorian month/year.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalenderViewHolder> {
    private final Context context;
    private final ArrayList<String> gregDaysOfMonth;
    private final ArrayList<String> drikDaysOfMonth;
    private final ArrayList<List<Integer>> drikImgIDOfMonth;
    private final ArrayList<String> drikMaasam;
    private final HashMap<Integer, RecyclerView.ViewHolder> holderlist;
    public final OnItemListener onItemListener;
    private final GradientDrawable gradientDrawable;

    public CalendarAdapter(Context context, ArrayList<String> gregDaysOfMonth,
                           ArrayList<String> drikDaysOfMonth,
                           ArrayList<List<Integer>> drikImgIDOfMonth,
                           ArrayList<String> drikMaasam,
                           OnItemListener onItemListener) {
        this.context = context;
        this.gregDaysOfMonth = gregDaysOfMonth;
        this.drikDaysOfMonth = drikDaysOfMonth;
        this.drikImgIDOfMonth = drikImgIDOfMonth;
        this.drikMaasam = drikMaasam;
        this.onItemListener = onItemListener;
        holderlist = new HashMap<>();

        gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(context.getResources().getColor(R.color.lightergray));
        gradientDrawable.setStroke(1, Color.BLUE);
        gradientDrawable.setGradientRadius(0.1F);
    }

    @NonNull
    @Override
    public CalenderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.calendar_cell, parent, false);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = (int)(parent.getHeight() * 0.166666666);
        return new CalenderViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CalenderViewHolder holder, int position) {
        holder.gregDaysOfMonth.setText(gregDaysOfMonth.get(position));
        holder.drikDaysOfMonth.setText(drikDaysOfMonth.get(position));
        List<Integer> imgIDs = drikImgIDOfMonth.get(position);

        String strMaasam = drikMaasam.get(position);
        if (strMaasam.contains("Adhik")) {
            holder.parentView.setBackground(gradientDrawable);
        }

        try {
            if ((imgIDs != null) && (imgIDs.size() > 0)) {
                int iconID = imgIDs.get(0);
                int firstIconID = iconID;
                if (iconID != -1) {
                    holder.drikCalendarImg1View.setImageResource(iconID);
                }

                if (imgIDs.size() == 4) {
                    // Load 2nd icon for 2nd imageview
                    iconID = imgIDs.get(1);
                    if (iconID != -1) {
                        holder.drikCalendarImg2View.setImageResource(iconID);
                    }

                    // Load 3rd icon for 3rd imageview
                    iconID = imgIDs.get(2);
                    if (iconID != -1) {
                        holder.drikCalendarImg3View.setImageResource(iconID);
                    }

                    // Load 4th icon for 4th imageview
                    iconID = imgIDs.get(3);
                    if (iconID != -1) {
                        holder.drikCalendarImg4View.setImageResource(iconID);
                    }
                } else if (imgIDs.size() == 3) {
                    // Load 2nd icon for 2nd imageview
                    iconID = imgIDs.get(1);
                    if (iconID != -1) {
                        holder.drikCalendarImg2View.setImageResource(iconID);
                    }

                    // Load 3rd icon for 3rd imageview
                    iconID = imgIDs.get(2);
                    if (iconID != -1) {
                        holder.drikCalendarImg3View.setImageResource(iconID);
                        holder.drikCalendarImg4View.setImageResource(iconID);
                    }
                } else if (imgIDs.size() == 2) {
                    iconID = imgIDs.get(1);
                    if (iconID != -1) {
                        // Load 2nd icon for 2nd imageview if dhina visheshams is 2
                        holder.drikCalendarImg2View.setImageResource(iconID);

                        // Load 2nd icon for 3rd imageview as well as this may show empty icon
                        // during flipping!
                        holder.drikCalendarImg3View.setImageResource(iconID);
                        holder.drikCalendarImg4View.setImageResource(firstIconID);
                    }
                }
                if (imgIDs.size() > 1) {
                    holder.viewFlipper.startFlipping();
                }
            }
            if (!holderlist.containsKey(position)) {
                holderlist.put(position, holder);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return gregDaysOfMonth.size();
    }

    public interface OnItemListener {
        void onItemClick (View view, int position);
    }
}
