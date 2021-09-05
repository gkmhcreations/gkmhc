package com.gkmhc.vedanta.nithya_panchangam;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class NithyaKaalam extends AppCompatActivity {
    private TextView nazhigaiClock;
    private static Timer nazhigaiTimer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
            getSupportActionBar().setLogo(R.mipmap.ic_launcher_round);
            getSupportActionBar().setBackgroundDrawable(
                    ResourcesCompat.getDrawable(getResources(), R.drawable.default_background, null));
            getSupportActionBar().setTitle(Html.fromHtml("<font color='#0000FF'>" +
                    getString(R.string.app_name) + "</font>"));
        } catch (Exception e) {
            // Nothing to do here.
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nithya_kaalam);
        this.getWindow().getDecorView().setBackgroundColor(
                getResources().getColor(android.R.color.holo_red_dark));

        nazhigaiClock = findViewById(R.id.np_kaalam_nazhigai_time);
        refreshTime();
    }

    @Override
    protected void onStop() {
        if (nazhigaiTimer != null) {
            nazhigaiTimer.cancel();
            nazhigaiTimer = null;
        }
        super.onStop();
    }

    private void refreshTime() //Call this method to refresh time
    {
        nazhigaiTimer = new Timer();
        nazhigaiTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    Calendar calendar = Calendar.getInstance();
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int min = calendar.get(Calendar.MINUTE);
                    int sec = calendar.get(Calendar.SECOND);

                    double dNazhigaiVal = (hour * 60) + min + (double)(sec / 60);
                    dNazhigaiVal /= 24;
                    double dVinazhigai = (dNazhigaiVal - (int)dNazhigaiVal) * 60;
                    double dTharparai = (dVinazhigai - (int)dVinazhigai) * 60;
                    nazhigaiClock.setText((int)dNazhigaiVal + ":" + (int)dVinazhigai + ":" + (int)dTharparai);
                });
            }
        }, 0, 1000);
    }
}