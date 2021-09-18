package com.gkmhc.vedanta.nithya_panchangam;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gkmhc.utils.VedicCalendar;

import java.util.ArrayList;

/**
 * Panchangam Adapter to organize Panchangam Information in ListView format.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class PanchangamAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> panchangamFields;
    private final ArrayList<String> panchangamValues;
    private final ArrayList<VedicCalendar.LagnamHoraiInfo> lagnamStr;
    private final ArrayList<VedicCalendar.LagnamHoraiInfo> lagnamFullDayList;
    private final ArrayList<VedicCalendar.LagnamHoraiInfo> horaiStr;
    private final ArrayList<VedicCalendar.LagnamHoraiInfo> horaiFullDayList;

    public PanchangamAdapter(Context context,
                             ArrayList<String> panchangamFields,
                             ArrayList<String> panchangamValues,
                             ArrayList<VedicCalendar.LagnamHoraiInfo> lagnamStr,
                             ArrayList<VedicCalendar.LagnamHoraiInfo> lagnamFullDayList,
                             ArrayList<VedicCalendar.LagnamHoraiInfo> horaiStr,
                             ArrayList<VedicCalendar.LagnamHoraiInfo> horaiFullDayList) {
        super(context, R.layout.panjangam_row, R.id.panchangam_field, panchangamFields);
        this.context = context;
        this.panchangamFields = panchangamFields;
        this.panchangamValues = panchangamValues;
        this.lagnamStr = lagnamStr;
        this.lagnamFullDayList = lagnamFullDayList;
        this.horaiStr = horaiStr;
        this.horaiFullDayList = horaiFullDayList;
    }

    @Override
    public int getPosition(@Nullable String item) {
        return super.getPosition(item);
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return panchangamFields.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        if (panchangamFields != null) {
            return panchangamFields.size();
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

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        PanchangamViewHolder panchangamViewHolder;
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.panjangam_row, null);
            panchangamViewHolder = new PanchangamViewHolder(convertView);
            convertView.setTag(panchangamViewHolder);
        } else {
            panchangamViewHolder = (PanchangamViewHolder) convertView.getTag();
        }

        float defTextSize = 16f;
        /*String selLocale = MainActivity.updateSelLocale(context.getApplicationContext());
        if (selLocale.equalsIgnoreCase("Sa")) {
            defTextSize = 18f;
        }*/

        panchangamViewHolder.panchangamField.setText(panchangamFields.get(position));
        panchangamViewHolder.panchangamField.setTextSize(defTextSize);

        // Make Lagnam value clickable to display a range of lagnams throughout the day!
        if (position == 16) {
            // Add Lagnam details
            // Display Lagnam from the list of Lagnams retrieved.
            StringBuilder lagnamVal = new StringBuilder();
            for (int index = 0;index < lagnamStr.size();index++) {
                VedicCalendar.LagnamHoraiInfo lagnamInfo = lagnamStr.get(index);
                lagnamVal.append(lagnamInfo.name);
                if (!lagnamInfo.timeValue.isEmpty()) {
                    lagnamVal.append(" (");
                    lagnamVal.append(lagnamInfo.timeValue);
                    lagnamVal.append(") ");
                    lagnamVal.append(VedicCalendar.ARROW_SYMBOL);
                }
            }
            panchangamViewHolder.panchangamValue.setText(lagnamVal.toString());
            panchangamViewHolder.panchangamValue.setTextSize(defTextSize);
            panchangamViewHolder.panchangamValue.setPaintFlags(
                    panchangamViewHolder.panchangamValue.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            /*panchangamViewHolder.panchangamValue.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.click_here, 0);*/
            convertView.setOnClickListener(v -> showLagnamDetails());
        }
        // Make Horai value clickable to display a range of horai(s) throughout the day!
        else if (position == 17) {
            // Add Horai details
            // Display Horai from the list of Horai(s) retrieved.
            StringBuilder horaiVal = new StringBuilder();
            for (int index = 0;index < horaiStr.size();index++) {
                VedicCalendar.LagnamHoraiInfo horaiInfo = horaiStr.get(index);

                // Remove the "line break" for better display
                String horaiParsedVal = horaiInfo.name.replaceAll("<br>", "");
                horaiVal.append(horaiParsedVal);
                if (!horaiInfo.timeValue.isEmpty()) {
                    horaiVal.append(" (");
                    horaiVal.append(horaiInfo.timeValue);
                    horaiVal.append(") ");
                    horaiVal.append(VedicCalendar.ARROW_SYMBOL);
                }
            }
            panchangamViewHolder.panchangamValue.setText(Html.fromHtml(horaiVal.toString()));
            panchangamViewHolder.panchangamValue.setTextSize(defTextSize);
            panchangamViewHolder.panchangamValue.setPaintFlags(
                    panchangamViewHolder.panchangamValue.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            /*panchangamViewHolder.panchangamValue.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.click_here, 0);*/
            convertView.setOnClickListener(v -> showHoraiDetails());
        }
        // Align & display "Nalla Neram" in HTML format
        else if (position == 19) {
            // Add "Nalla Neram" details
            String nallaNeramParsedVal = panchangamValues.get(position);

            // Replace only the last occurence of "line break"
            nallaNeramParsedVal = nallaNeramParsedVal.replaceAll("<br>$", "");
            panchangamViewHolder.panchangamValue.setText(Html.fromHtml(nallaNeramParsedVal));
            panchangamViewHolder.panchangamValue.setTextSize(defTextSize);
        }
        // Display remaining fields as-is as retrived!
        else {
            // Default handling for remaining panchangam fields & values.
            panchangamViewHolder.panchangamValue.setText(panchangamValues.get(position));
            panchangamViewHolder.panchangamValue.setTextSize(defTextSize);
        }
        return convertView;
    }

    // Show full-day horai(s) in a tabular form
    private void showHoraiDetails() {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View horaiView = layoutInflater.inflate(R.layout.lagnam_horai_layout, null);
        TableLayout lagnamTable = horaiView.findViewById(R.id.lagnam_horai_table);
        TextView titleView = horaiView.findViewById(R.id.lagnam_horai_hdr);
        titleView.setText(R.string.horai_details);
        TextView tblHdrTextView = horaiView.findViewById(R.id.lagnam_horai_table_hdr);
        tblHdrTextView.setText(R.string.horai_heading);
        String startTime = "";

        // Add each lagnam row to the table.
        for (int index = 0;index < horaiFullDayList.size();index++) {
            VedicCalendar.LagnamHoraiInfo horaiInfo = horaiFullDayList.get(index);

            TableRow row = new TableRow(getContext());
            TableRow.LayoutParams lp = new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
            lp.setMargins(1, 1, 1, 1);
            row.setLayoutParams(lp);

            GradientDrawable border = new GradientDrawable();
            border.setColor(context.getResources().getColor(R.color.lightSaffron));
            border.setStroke(1, Color.BLUE);

            // Create a textview & switchcompat fields for each alarm
            if (horaiInfo.isCurrent) {
                border.setColor(context.getResources().getColor(R.color.white));
            }
            TextView tv1 = new TextView(getContext());
            tv1.setText(Html.fromHtml(horaiInfo.name));
            tv1.setTextColor(Color.BLUE);
            tv1.setMaxLines(1);
            lp.weight = (float) 0.34;
            tv1.setLayoutParams(lp);
            tv1.setGravity(Gravity.START);
            row.addView(tv1);

            TextView tv2 = new TextView(getContext());
            tv2.setText(startTime);
            tv2.setTextColor(Color.BLUE);
            tv2.setMaxLines(1);
            lp.weight = (float) 0.33;
            tv2.setLayoutParams(lp);
            tv2.setGravity(Gravity.CENTER_HORIZONTAL);
            row.addView(tv2);

            TextView tv3 = new TextView(getContext());
            tv3.setText(horaiInfo.timeValue);
            startTime = horaiInfo.timeValue;
            tv3.setTextColor(Color.BLUE);
            tv3.setMaxLines(1);
            lp.weight = (float) 0.33;
            tv3.setLayoutParams(lp);
            tv3.setGravity(Gravity.CENTER_HORIZONTAL);
            row.addView(tv3);
            row.setBackground(border);
            lagnamTable.addView(row);
        }

        AlertDialog alertDialog =
                new AlertDialog.Builder(getContext()).setView(horaiView).create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.holo_red_dark);
        alertDialog.show();
    }

    // Show full-day lagnam(s) in a tabular form
    private void showLagnamDetails() {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View lagnamView = layoutInflater.inflate(R.layout.lagnam_horai_layout, null);
        TableLayout lagnamTable = lagnamView.findViewById(R.id.lagnam_horai_table);
        String startTime = "";

        VedicCalendar.LagnamHoraiInfo lastLagnamInfo =
                lagnamFullDayList.get(lagnamFullDayList.size() - 1);
        if (lastLagnamInfo != null) {
            startTime = lastLagnamInfo.timeValue;
        }

        // Add each lagnam row to the table.
        for (int index = 0;index < lagnamFullDayList.size();index++) {
            VedicCalendar.LagnamHoraiInfo lagnamInfo = lagnamFullDayList.get(index);

            TableRow row = new TableRow(getContext());
            TableRow.LayoutParams lp = new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
            lp.setMargins(1, 1, 1, 1);
            row.setLayoutParams(lp);

            GradientDrawable border = new GradientDrawable();
            border.setColor(context.getResources().getColor(R.color.lightSaffron));
            border.setStroke(1, Color.BLUE);

            // Create a textview & switchcompat fields for each alarm
            if (lagnamInfo.isCurrent) {
                border.setColor(context.getResources().getColor(R.color.white));
            }
            TextView tv1 = new TextView(getContext());
            tv1.setText(lagnamInfo.name);
            tv1.setTextColor(Color.BLUE);
            tv1.setMaxLines(1);
            lp.weight = (float) 0.34;
            tv1.setLayoutParams(lp);
            tv1.setGravity(Gravity.START);
            row.addView(tv1);

            TextView tv2 = new TextView(getContext());
            tv2.setText(startTime);
            tv2.setTextColor(Color.BLUE);
            tv2.setMaxLines(1);
            lp.weight = (float) 0.33;
            tv2.setLayoutParams(lp);
            tv2.setGravity(Gravity.CENTER_HORIZONTAL);
            row.addView(tv2);

            TextView tv3 = new TextView(getContext());
            tv3.setText(lagnamInfo.timeValue);
            startTime = lagnamInfo.timeValue;
            tv3.setTextColor(Color.BLUE);
            tv3.setMaxLines(1);
            lp.weight = (float) 0.33;
            tv3.setLayoutParams(lp);
            tv3.setGravity(Gravity.CENTER_HORIZONTAL);
            row.addView(tv3);
            row.setBackground(border);
            lagnamTable.addView(row);
        }

        AlertDialog alertDialog =
                new AlertDialog.Builder(getContext()).setView(lagnamView).create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.holo_red_dark);
        alertDialog.show();
    }
}
