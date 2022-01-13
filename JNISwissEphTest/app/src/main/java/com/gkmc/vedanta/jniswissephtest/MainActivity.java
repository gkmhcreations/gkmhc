package com.gkmc.vedanta.jniswissephtest;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import swisseph.SwissEph;

public class MainActivity extends AppCompatActivity {
    private static int MAX_24HOURS = 24;
    private static int MAX_MINS_IN_HOUR = 60;
    private static int MAX_MINS_IN_DAY = 1440;
    private static double defTimezone = 5.5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getTithiSpan(0, 12);
    }

    private double getTithiSpan (int thithiIndex, int deg) {
        SweDate sd = new SweDate(2021, 12, 25, 0);
        int flags = SweConst.SEFLG_SWIEPH | SweConst.SEFLG_SIDEREAL |
                SweConst.SEFLG_TRANSIT_LONGITUDE;

        TransitCalculator tcEnd = new TCPlanetPlanet(swissEphInst, SweConst.SE_MOON, SweConst.SE_SUN,
                flags, 0);
        double tithiDeg = 0;
        tithiDeg += (thithiIndex * deg); // 12 deg is one thithi (or) 6 deg for karanam
        tcEnd.setOffset(tithiDeg);

        return getSDTimeZone(sd.getJulDay(),
                swissEphInst.getTransitUT(tcEnd, sd.getJulDay(), false));
    }

    private static double getSDTimeZone(double jdFrom, double jdTo) {
        double diff = (jdTo - jdFrom) * MAX_24HOURS;
        diff += defTimezone;
        diff *= MAX_MINS_IN_HOUR;
        if (diff < 0) {
            diff += MAX_MINS_IN_DAY;
        }
        return diff;
    }
}