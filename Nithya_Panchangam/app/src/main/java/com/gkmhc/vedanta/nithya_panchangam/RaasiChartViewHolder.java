package com.gkmhc.vedanta.nithya_panchangam;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RaasiChartViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public final TextView raasiDescription;
    private final CalendarAdapter.OnItemListener onItemListener;

    RaasiChartViewHolder(@NonNull View itemView,
                         CalendarAdapter.OnItemListener onItemListener) {
        super(itemView);
        raasiDescription = itemView.findViewById(R.id.raasi_cell_description);
        this.onItemListener = onItemListener;
        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        onItemListener.onItemClick(view, getAdapterPosition());
    }
}
