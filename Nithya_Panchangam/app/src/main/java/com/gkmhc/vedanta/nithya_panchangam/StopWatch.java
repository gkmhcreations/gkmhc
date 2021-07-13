package com.gkmhc.vedanta.nithya_panchangam;

import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Stopwatch fragment to handle start, pause & stop functions of a chronometer.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class StopWatch extends Fragment {
    private TextView chronometer;
    private boolean isAlarmRunning = false;
    ImageButton btnStart;
    ImageButton btnStop;
    Handler handler;
    long tMs, tStart, tPauseOffset = 0L;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_stop_watch, container, false);

        // Textview is good enough. We don't need a chronometer!
        chronometer = root.findViewById(R.id.chronometer);
        btnStart = root.findViewById(R.id.btn_alarm_start);
        btnStop = root.findViewById(R.id.btn_alarm_stop);

        btnStart.setOnClickListener(v -> {
            // Alarm Not Running? Yes
            //  - Start stopwatch accounting for when last stopwatch was paused so that
            //    alarm can resume from where it was paused or start from ZERO.
            //  - Pause button to be made visible when stopwatch is about to be started
            //  - Hide stop button as it should not be clickable
            if (!isAlarmRunning) {
                tStart = SystemClock.elapsedRealtime();
                handler.postDelayed(runnable, 0);
                isAlarmRunning = true;
                btnStop.setVisibility(View.GONE);
                btnStart.setImageDrawable(
                        ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, null));
            } else {
                // Alarm Running? Yes
                //  - Stop the stopwatch
                //  - Note down pauseOffset so that alarm can resume from this offset
                //  - Make play button visible
                //  - Make stop button visible
                tPauseOffset += tMs;
                handler.removeCallbacks(runnable);
                isAlarmRunning = false;
                btnStop.setVisibility(View.VISIBLE);
                btnStart.setImageDrawable(
                        ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, null));
            }
        });

        // Alarm Running? No
        //  - Reset pauseOffset to ZERO
        //  - Make play button visible
        // Note: Cannot stop alarm when running. It can only by paused.
        btnStop.setOnClickListener(v -> {
            if (!isAlarmRunning) {
                tMs = tStart = tPauseOffset = 0L;
                btnStart.setImageDrawable(
                        ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, null));
                chronometer.setText(getString(R.string.def_stopwatch_value));
            }
        });

        // If handler is still alive, then just keep it running.
        // But, show the right buttons.
        if (handler != null) {
            if (isAlarmRunning) {
                btnStop.setVisibility(View.GONE);
                btnStart.setImageDrawable(
                        ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, null));
            } else {
                updateChronometer(tPauseOffset);
            }
        } else {
            handler = new Handler();
        }

        // Inflate the layout for this fragment
        return root;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // Create a separate running thread to update chronometer at 60ms periodicity!
    public final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            tMs = SystemClock.elapsedRealtime() - tStart;
            updateChronometer(tPauseOffset + tMs);
            handler.postDelayed(this, 60);
        }
    };

    // Update Chronometer in MM:SS:MS format
    public void updateChronometer (long elapsedTime) {
        int ms = (int)(elapsedTime);
        int sec = ms / 1000;
        int min = sec / 60;
        sec %= 60;
        ms %= 1000;
        chronometer.setText(String.format("%02d:%02d:%03d", min, sec, ms));
    }
}