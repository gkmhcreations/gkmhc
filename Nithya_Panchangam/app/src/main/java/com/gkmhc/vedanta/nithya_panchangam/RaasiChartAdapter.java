package com.gkmhc.vedanta.nithya_panchangam;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gkmhc.utils.VedicCalendar;

import java.util.ArrayList;
import java.util.HashMap;

public class RaasiChartAdapter extends RecyclerView.Adapter<RaasiChartViewHolder> {
    private final Context context;
    private final String []raasiList;
    private final String []planetList;
    HashMap<Integer, Double> planetsRiseTimings;
    private final GradientDrawable gradientDrawable;
    public final CalendarAdapter.OnItemListener onItemListener;
    private final int RAASI_CHART_POSITION_MESHAM = 1;
    private final int RAASI_CHART_POSITION_RISHABAM = 2;
    private final int RAASI_CHART_POSITION_MITHUNAM = 3;
    private final int RAASI_CHART_POSITION_KATAKAM = 7;
    private final int RAASI_CHART_POSITION_SIMHAM = 11;
    private final int RAASI_CHART_POSITION_KANNI = 15;
    private final int RAASI_CHART_POSITION_THULAM = 14;
    private final int RAASI_CHART_POSITION_VRICHIGAM = 13;
    private final int RAASI_CHART_POSITION_DHANUSU = 12;
    private final int RAASI_CHART_POSITION_MAKARAM = 8;
    private final int RAASI_CHART_POSITION_KUMBHAM = 4;
    private final int RAASI_CHART_POSITION_MEENAM = 0;
    private final HashMap<Integer, String> raasiDescrChartPos;

    public RaasiChartAdapter(Context context, String []raasiList, String []planetList,
                             HashMap<Integer, Double> planetsRiseTimings,
                             CalendarAdapter.OnItemListener onItemListener) {
        this.context = context;
        this.raasiList = raasiList;
        this.planetList = planetList;
        this.planetsRiseTimings = planetsRiseTimings;
        this.onItemListener = onItemListener;

        raasiDescrChartPos = new HashMap<>();
        for (int index = 0;index < planetsRiseTimings.size();index++) {
            String planetName = planetList[index];
            double planetLongitude = planetsRiseTimings.get(index);
            int planetPos = (int) (planetLongitude / 1800);
            int chartPos = getChartPos(planetPos);
            String planetInRaasi = raasiDescrChartPos.get(chartPos);
            if (planetInRaasi != null) {
                planetInRaasi += "<br>" + planetName;
            } else {
                planetInRaasi = planetName;
            }
            raasiDescrChartPos.put(chartPos, planetInRaasi);
        }

        gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(context.getResources().getColor(R.color.lightSaffron));
        gradientDrawable.setStroke(1, Color.BLUE);
        gradientDrawable.setGradientRadius(0.1F);

        /*raasiDescrChartPos.put(RAASI_CHART_POSITION_MESHAM, raasiList[0]);
        raasiDescrChartPos.put(RAASI_CHART_POSITION_RISHABAM, raasiList[1]);
        raasiDescrChartPos.put(RAASI_CHART_POSITION_MITHUNAM, raasiList[2]);
        raasiDescrChartPos.put(RAASI_CHART_POSITION_KATAKAM, raasiList[3]);
        raasiDescrChartPos.put(RAASI_CHART_POSITION_SIMHAM, raasiList[4]);
        raasiDescrChartPos.put(RAASI_CHART_POSITION_KANNI, raasiList[5]);
        raasiDescrChartPos.put(RAASI_CHART_POSITION_THULAM, raasiList[6]);
        raasiDescrChartPos.put(RAASI_CHART_POSITION_VRICHIGAM, raasiList[7]);
        raasiDescrChartPos.put(RAASI_CHART_POSITION_DHANUSU, raasiList[8]);
        raasiDescrChartPos.put(RAASI_CHART_POSITION_MAKARAM, raasiList[9]);
        raasiDescrChartPos.put(RAASI_CHART_POSITION_KUMBHAM, raasiList[10]);
        raasiDescrChartPos.put(RAASI_CHART_POSITION_MEENAM, raasiList[11]);*/
    }

    @NonNull
    @Override
    public RaasiChartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.raasi_cell, parent, false);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = (int)(parent.getHeight() * 0.25);
        return new RaasiChartViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RaasiChartViewHolder holder, int position) {
        String raasiForPos = "";
        if (raasiDescrChartPos.containsKey(position)) {
            raasiForPos = raasiDescrChartPos.get(position);
        }
        if (position < raasiList.length) {
            if ((position != 5) && (position != 6) && (position != 9) && (position != 10)) {
                holder.raasiDescription.setText(Html.fromHtml(raasiForPos));
                holder.itemView.setBackground(gradientDrawable);
            }
        } else {
            holder.raasiDescription.setText(Html.fromHtml(raasiForPos));
            holder.itemView.setBackground(gradientDrawable);
        }
    }

    @Override
    public int getItemCount() {
        return 16;
    }

    private int getChartPos(int planetPos) {
        int chartPos = RAASI_CHART_POSITION_MEENAM;
        switch (planetPos) {
            case 0:
                chartPos = RAASI_CHART_POSITION_MESHAM;
                break;
            case 1:
                chartPos = RAASI_CHART_POSITION_RISHABAM;
                break;
            case 2:
                chartPos = RAASI_CHART_POSITION_MITHUNAM;
                break;
            case 3:
                chartPos = RAASI_CHART_POSITION_KATAKAM;
                break;
            case 4:
                chartPos = RAASI_CHART_POSITION_SIMHAM;
                break;
            case 5:
                chartPos = RAASI_CHART_POSITION_KANNI;
                break;
            case 6:
                chartPos = RAASI_CHART_POSITION_THULAM;
                break;
            case 7:
                chartPos = RAASI_CHART_POSITION_VRICHIGAM;
                break;
            case 8:
                chartPos = RAASI_CHART_POSITION_DHANUSU;
                break;
            case 9:
                chartPos = RAASI_CHART_POSITION_MAKARAM;
                break;
            case 10:
                chartPos = RAASI_CHART_POSITION_KUMBHAM;
                break;
            case 11:
                chartPos = RAASI_CHART_POSITION_MEENAM;
                break;
        }

        return chartPos;
    }
}
