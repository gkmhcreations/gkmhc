package com.gkmhc.vedanta.nithya_panchangam;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.gkmhc.utils.VedicCalendar;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RaasiChart extends AppCompatActivity implements
        CalendarAdapter.OnItemListener {
    private static final String PREF_NP_LOCALE_KEY = "PREF_NP_LOCALE_KEY";
    private static final String PREF_LOCATION_DEF_VAL_KEY = "PREF_LOCATION_DEF_VAL_KEY";
    public static final String EXTRA_DATE = "Raasi_Extra_Date";
    public static final String EXTRA_MONTH = "Raasi_Extra_Month";
    public static final String EXTRA_YEAR = "Raasi_Extra_Year";
    private double curLocationLongitude = 0;
    private double curLocationLatitude = 0;
    private RecyclerView raasiRecyclerView;
    private Calendar currCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String prefLang = readLocaleSettings();
        String selLocale = MainActivity.getLocaleShortStr(prefLang);
        Locale locale = new Locale(selLocale);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        currCalendar = Calendar.getInstance();
        Intent recvdIntent = this.getIntent();
        if (recvdIntent.hasExtra(EXTRA_DATE)) {
            int date = recvdIntent.getIntExtra(EXTRA_DATE, currCalendar.get(Calendar.DATE));
            int month = recvdIntent.getIntExtra(EXTRA_DATE, currCalendar.get(Calendar.MONTH));
            int year = recvdIntent.getIntExtra(EXTRA_DATE, currCalendar.get(Calendar.YEAR));

            currCalendar.set(year, month, date);
        }

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

        String defLocation = MainActivity.readDefLocationSetting(this);
        if (defLocation.isEmpty()) {
            defLocation = getString(R.string.pref_def_location_val);
        }
        getLocationCoords(defLocation);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raasi_chart);
        this.getWindow().getDecorView().setBackgroundColor(
                getResources().getColor(android.R.color.holo_red_dark));
        raasiRecyclerView = findViewById(R.id.np_raasi_recycler_view);
        refreshRaasiView(defLocation);
    }

    public void getLocationCoords(String locationStr) {
        /*try {
            Geocoder geocoder = new Geocoder(this);
            List<Address> addressList = geocoder.getFromLocationName(locationStr, 1);
            if ((addressList != null) && (addressList.size() > 0)) {
                String strLocality = addressList.get(0).getLocality();
                curLocationLongitude = addressList.get(0).getLongitude();
                curLocationLatitude = addressList.get(0).getLatitude();
                Log.d("NPCalendar", "Location: " + strLocality +
                        " Longitude: " + curLocationLongitude +
                        " Latitude: " + curLocationLatitude);
            }
        } catch (Exception e) {
            // Nothing to do here.
            Log.d("NPCalendar","Exception in initManualLocation()");
        }*/
        MainActivity.PlacesInfo placesInfo = MainActivity.getLocationFromPlacesDB(locationStr);
        curLocationLatitude = placesInfo.latitude;
        curLocationLongitude = placesInfo.longitude;
    }

    private String readLocaleSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences != null) {
            return sharedPreferences.getString(PREF_NP_LOCALE_KEY, "En");
        }
        return "En";
    }

    public void refreshRaasiView(String defLocation) {
        new Handler().postDelayed(() -> {
            // code runs in a thread
            updateRaasiView(defLocation);
        }, 10);
    }

    private void updateRaasiView(String defLocation) {
        String[] raasiList = getResources().getStringArray(R.array.raasi_list);
        String[] planetList = getResources().getStringArray(R.array.planet_list);

        long startTime = System.nanoTime();
        HashMap<String, String[]> vedicCalendarLocaleList =
                MainActivity.buildVedicCalendarLocaleList(this);
        int ayanamsaMode = MainActivity.readPrefAyanamsaSelection(this);
        VedicCalendar vedicCalendar = VedicCalendar.getInstance(
                VedicCalendar.PANCHANGAM_TYPE_DRIK_GANITHAM, currCalendar, curLocationLongitude,
                curLocationLatitude, MainActivity.getLocationTimeZone(defLocation), ayanamsaMode,
                vedicCalendarLocaleList);
        HashMap<Integer, Double> planetsRiseTimings = vedicCalendar.getPlanetsRise();
        long endTime = System.nanoTime();
        System.out.println("RaasiChart, Time Taken: " + VedicCalendar.getTimeTaken(startTime, endTime));

        RaasiChartAdapter raasiChartAdapter = new RaasiChartAdapter(this, raasiList, planetList,
                planetsRiseTimings, this);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 4);
        raasiRecyclerView.setHasFixedSize(true);
        raasiRecyclerView.setLayoutManager(layoutManager);
        raasiRecyclerView.setAdapter(raasiChartAdapter);
        raasiChartAdapter.notifyDataSetChanged();

        String degLatSuffix = "°N";
        if (curLocationLatitude < 0) {
            degLatSuffix = "°S";
        }

        String degLongSuffix = "°E";
        if (curLocationLongitude < 0) {
            degLongSuffix = "°W";
        }

        TextView textView = findViewById(R.id.raasi_location_details);
        String npRaasiText = defLocation + " (" +
                String.format("%.5g%n", Math.abs(curLocationLatitude)) + degLatSuffix + ", " +
                String.format("%.5g%n", Math.abs(curLocationLongitude)) + degLongSuffix + ")";
        npRaasiText = npRaasiText.replace("\n", "");
        textView.setText(npRaasiText);
        npRaasiText = " (" + currCalendar.get(Calendar.DATE) + "-" +
                currCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                        Locale.ENGLISH) + "-" + currCalendar.get(Calendar.YEAR) + ")";
        textView = findViewById(R.id.raasi_day_details);
        textView.setText(npRaasiText);
    }

    @Override
    public void onItemClick(View view, int position) {

    }
}