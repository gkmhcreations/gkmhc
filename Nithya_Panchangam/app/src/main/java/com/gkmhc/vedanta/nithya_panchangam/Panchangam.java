package com.gkmhc.vedanta.nithya_panchangam;

import android.app.AlertDialog;
import android.graphics.Paint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gkmhc.utils.VedicCalendar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Panchangam fragment that retrieves all panchangam details for given day & displays the same.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class Panchangam extends Fragment {
    private MainActivity mainActivity;
    private Calendar selectedCalendar;
    private View root;
    private ListView panchangamListView;
    private ArrayList<String> panchangamFields;
    private ArrayList<String> panchangamValues;

    private String maasamStr;
    private int refDinaAnkam = 0;
    private String vaasaramStr;
    private ArrayList<VedicCalendar.LagnamHoraiInfo> lagnamStr;
    private ArrayList<VedicCalendar.LagnamHoraiInfo> lagnamFullDayList;
    private ArrayList<VedicCalendar.LagnamHoraiInfo> horaiStr;
    private ArrayList<VedicCalendar.LagnamHoraiInfo> horaiFullDayList;
    private String curLocationCity = "";
    private double curLocationLongitude = 0; // Default to Varanasi
    private double curLocationLatitude = 0; // Default to Varanasi
    private TextView textViewCurLocation = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.updateAppLocale();
        }

        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_panchangam, container, false);
        panchangamListView = root.findViewById(R.id.panchangam_table);

        // Handle Prev Date Button
        // Get Prev Date panchangam & update fragment
        FloatingActionButton faBtnDayPrev = root.findViewById(R.id.npDayPrev);
        faBtnDayPrev.setOnClickListener(v -> {
            Calendar prevCalendar = mainActivity.getSelectedCalendar();
            prevCalendar.add(Calendar.DATE, -1);
            int refDate = prevCalendar.get(Calendar.DATE);
            int refMonth = prevCalendar.get(Calendar.MONTH);
            int refYear = prevCalendar.get(Calendar.YEAR);
            mainActivity.setSelectedCalendar(refDate, refMonth, refYear);

            refreshPanchangam(true);
            mainActivity.refreshTab(NPAdapter.NP_TAB_SANKALPAM);
        });

        // Handle Next Date Button
        // Get Next Date panchangam & update fragment
        FloatingActionButton faBtnDayNext = root.findViewById(R.id.npDayNext);
        faBtnDayNext.setOnClickListener(v -> {
            Calendar nextCalendar = mainActivity.getSelectedCalendar();
            nextCalendar.add(Calendar.DATE, +1);
            int refDate = nextCalendar.get(Calendar.DATE);
            int refMonth = nextCalendar.get(Calendar.MONTH);
            int refYear = nextCalendar.get(Calendar.YEAR);
            mainActivity.setSelectedCalendar(refDate, refMonth, refYear);

            refreshPanchangam(true);
            mainActivity.refreshTab(NPAdapter.NP_TAB_SANKALPAM);
        });

        textViewCurLocation = root.findViewById(R.id.location);
        textViewCurLocation.setPaintFlags(textViewCurLocation.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        textViewCurLocation.setEnabled(true);

        textViewCurLocation.setOnClickListener(tView -> {
            mainActivity.updateAppLocale();
            View locationSelectView =
                    inflater.inflate(R.layout.change_manual_location, null);
            ArrayAdapter<String> arrayAdapter =
                    new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_list_item_1, MainActivity.placesList);
            AutoCompleteTextView autoCompleteTextView =
                    locationSelectView.findViewById(R.id.location_dropbox);
            autoCompleteTextView.setAdapter(arrayAdapter);
            AlertDialog alertDialog =
                    new AlertDialog.Builder(getContext())
                        .setCancelable(true)
                        .setView(locationSelectView).create();
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            autoCompleteTextView.setOnItemClickListener((adapterView, view, position, id) -> {
                String cityToSearch = (String)adapterView.getItemAtPosition(position);
                if (mainActivity.updateManualLocation(cityToSearch)) {
                    alertDialog.dismiss();
                    refreshPanchangam(true);
                } else {
                    Toast.makeText(getContext(), "Invalid Location: " + cityToSearch,
                            Toast.LENGTH_SHORT).show();
                }
            });
            autoCompleteTextView.requestFocus();
            alertDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            alertDialog.show();
        });

        if (mainActivity.isAppLaunchedFirstTime()) {
            mainActivity.updateAppLaunchedFirstTime();
            textViewCurLocation.performClick();
        }

        //SwipeListener swipeListener = new SwipeListener(root);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            refreshPanchangam(false);
        } catch (Exception exception) {
            Log.e("Panchangam","Exception --- onResume()!");
        }
    }

    /**
     * Use this utility function to update the Panchangam details for the given Calendar date.
     *
     * @param forceRefresh True to force refresh, false to just update partially.
     */
    public void refreshPanchangam(boolean forceRefresh) {
        new Handler().postDelayed(() -> {
            try {
                // code runs in a thread
                boolean toRefresh;
                Calendar calendar = mainActivity.getSelectedCalendar();
                if (forceRefresh) {
                    toRefresh = true;
                } else {
                    toRefresh = (selectedCalendar == null) ||
                            (calendar.get(Calendar.YEAR) != selectedCalendar.get(Calendar.YEAR)) ||
                            (calendar.get(Calendar.MONTH) != selectedCalendar.get(Calendar.MONTH)) ||
                            (calendar.get(Calendar.DATE) != selectedCalendar.get(Calendar.DATE));
                }

                // Refresh completely if set to true, else just update UI.
                String selLocale = mainActivity.updateAppLocale();
                if (toRefresh) {
                    selectedCalendar = calendar;
                    curLocationLongitude = mainActivity.getLongitude();
                    curLocationLatitude = mainActivity.getLatitude();
                    //Log.d("Panchangam","Longitude: " + curLocationLongitude +
                    //        " Latitude: " + curLocationLatitude);
                    retrieveTodaysPanchangam(selectedCalendar, selLocale);
                    updatePanchangamFieldsHeader();
                }
                updatePanchangamFragment(root, selectedCalendar, selLocale);
            } catch (final Exception ex) {
                Log.e("Panchangam","Exception in refreshPanchangam()");
            }
        }, 50);
    }

    /**
     * Use this utility function to update the current location in the Textview.
     *
     * @param inputLocation Location as input.
     */
    public void updateCurLocation(String inputLocation) {
        if (textViewCurLocation != null) {
            if (inputLocation.equals("")) {
                if (curLocationCity.equals("")) {
                    textViewCurLocation.setText(R.string.location_unknown);
                } else {
                    textViewCurLocation.setText(curLocationCity);
                }
            } else {
                curLocationCity = inputLocation;
                String[] location = curLocationCity.split(",");

                // Display only city and not country
                // Typical format of string would be Varanasi, India.
                if (location.length > 0) {
                    textViewCurLocation.setText(location[0]);
                } else {
                    textViewCurLocation.setText(inputLocation);
                }
            }
        } else {
            curLocationCity = inputLocation;
            Log.e("Panchangam","Updating Location...FAILED");
        }
    }

    /**
     * Use this utility function to retrieve all the values for Vedic Panchangam elements and
     * store them as data members.
     *
     * @param currCalendar A Calendar date as per Gregorian Calendar
     * @param selLocale    Language
     *        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
     *              If an unsupported locale is given as input, then by default "English" is
     *              assumed instead of return ERROR.
     */
    private void retrieveTodaysPanchangam(Calendar currCalendar, String selLocale) {
        // Create a VedicCalendar instance.
        // 1) Current Calendar - Today or any given date
        // 2) localpath - Path to SwissEph conf files
        // 3) Longitude - This is important as panchangam calculations changes with location
        // 4) Latitude - This is important as panchangam calculations changes with location
        long pStartTime = System.nanoTime();
        //long startTime = System.nanoTime();

        HashMap<String, String[]> vedicCalendarLocaleList =
                MainActivity.buildVedicCalendarLocaleList(getContext());
        String location = MainActivity.readDefLocationSetting(getContext());
        int ayanamsaMode = MainActivity.readPrefAyanamsaSelection(getContext());
        VedicCalendar vedicCalendar = VedicCalendar.getInstance(
                VedicCalendar.PANCHANGAM_TYPE_DRIK_GANITHAM, currCalendar, curLocationLongitude,
                curLocationLatitude, MainActivity.getLocationTimeZone(location), ayanamsaMode,
                vedicCalendarLocaleList);
        //long endTime = System.nanoTime();
        //Log.d("PanchangamProfiler","VedicCalendar()... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime, endTime));
        if (vedicCalendar != null) {
            panchangamValues = new ArrayList<>();

            // Step1: Calculate Samvatsaram
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getSamvatsaram());
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getSamvatsaram()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step2: Retrieve correct Ayanam given current system time
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getAyanam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getAyanam()... Time Taken: " +
          //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step3: Retrieve correct rithou  given current system time
            // Step5: Retrieve correct paksham  given current system time
            // Step6: Retrieve correct thithi  given current system time
            //startTime = System.nanoTime();
            refDinaAnkam = vedicCalendar.getDinaAnkam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getDinaankham()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step3: Retrieve correct rithou  given current system time
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getRithu(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getRithu()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step4: Retrieve correct maasam given current system time
            //startTime = System.nanoTime();
            maasamStr = vedicCalendar.getMaasam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
            panchangamValues.add(maasamStr);

            // For display purposes only!
            maasamStr = vedicCalendar.getMaasam(VedicCalendar.MATCH_SANKALPAM_EXACT);
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getMaasam()... " + maasamStr + " Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step5: Retrieve correct paksham  given current system time
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getPaksham());
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getPaksham()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step6: Retrieve correct thithi  given current system time
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getThithi(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getThithi()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step7: Retrieve correct vaasaram for the current thithi
            //startTime = System.nanoTime();
            vaasaramStr = vedicCalendar.getVaasaram(VedicCalendar.MATCH_SANKALPAM_EXACT);
            panchangamValues.add(vaasaramStr);
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getVaasaram()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step8: Retrieve correct raasi for the current thithi
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getRaasi(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getRaasi()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step9: Retrieve correct natchathiram for the current thithi
            //startTime = System.nanoTime();
            panchangamValues.add(
                    vedicCalendar.getNakshatram(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getNatchathiram()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step10: Retrieve correct Chandrashtama natchathiram for the current thithi
            //startTime = System.nanoTime();
            panchangamValues.add(
                    vedicCalendar.getChandrashtamaNakshatram(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getChandrashtamaNatchathiram()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step11: Retrieve correct lagnam for the current thithi
            //startTime = System.nanoTime();
            lagnamStr = vedicCalendar.getLagnam(VedicCalendar.MATCH_SANKALPAM_EXACT);
            panchangamValues.add("");
            lagnamFullDayList =
                    vedicCalendar.getLagnam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);

            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getLagnam()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step12: Retrieve correct horai for the current thithi
            //startTime = System.nanoTime();
            horaiStr = vedicCalendar.getHorai(VedicCalendar.MATCH_SANKALPAM_EXACT);
            panchangamValues.add("");
            horaiFullDayList =
                    vedicCalendar.getHorai(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getHorai()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step13: Retrieve correct Yogam for the current thithi
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getYogam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getYogam()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step14: Retrieve correct Karanam for the current thithi
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getKaranam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getKaranam()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step15: Retrieve amruthathi yogam in the current thithi
            //startTime = System.nanoTime();
            panchangamValues.add(
                    vedicCalendar.getAmruthathiYogam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getAmruthathiYogam()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step16: Retrieve Sunrise timings for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getSunrise());
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getSunrise()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step17: Retrieve Sunrise timings for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getSunset());
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getSunset()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step18: Retrieve Nalla Neram (auspicious time) within the current thithi
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getNallaNeram(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getNallaNeram()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step19: Retrieve Raahu Kaalam timings for the current vaasaram
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getRaahuKaalamTimings(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getRaahuKaalamTimings()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step20: Retrieve Yamakandam timings for the current vaasaram
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getYamakandamTimings(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getYamakandamTimings()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step21: Retrieve Kuligai timings for the current vaasaram
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getKuligaiTimings(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getKuligaiTimings()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            //startTime = System.nanoTime();
            List<Integer> dhinaVisheshamCodeList =
                    vedicCalendar.getDinaVishesham(VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
            String dhinaSpecialStr;
            if (dhinaVisheshamCodeList.size() > 0) {
                // Form a list of Dina Vishesham(s) in case there is more than 1 occuring in a
                // given calendar day.
                List<String> dhinaVisheshamStrList = new ArrayList<>();
                for (int code = 0;code < dhinaVisheshamCodeList.size();code++) {
                    int visheshamCode = dhinaVisheshamCodeList.get(code);
                    int labelID = Reminder.getDinaVisheshamLabel(visheshamCode);
                    dhinaVisheshamStrList.add(getString(labelID));
                }
                dhinaSpecialStr = dhinaVisheshamStrList.toString();
                dhinaSpecialStr = dhinaSpecialStr.substring(1, dhinaSpecialStr.length() - 1);
            } else {
                dhinaSpecialStr = "";
            }
            panchangamValues.add(0, dhinaSpecialStr);
            panchangamValues.add("");
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getDinaVishesham()..." + " Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            long pEndTime = System.nanoTime();
            Log.d("PanchangamProfiler","Overall Retrieve Time Taken: " +
                    VedicCalendar.getTimeTaken(pStartTime, pEndTime));
        }
    }

    /**
     * Utility function to update all elements in Panchangam fragment with all the values
     * retrieved from vedicCalendar mapped to the given Calendar date.
     *
     * @param root          Root View
     * @param currCalendar  A Calendar date as per Gregorian Calendar
     */
    private void updatePanchangamFragment(View root, Calendar currCalendar, String selLocale) {
        long pStartTime = System.nanoTime();
        PanchangamAdapter panchangamAdapter = new PanchangamAdapter(getContext(),
                panchangamFields, panchangamValues, lagnamStr, lagnamFullDayList, horaiStr,
                horaiFullDayList);
        panchangamListView.setAdapter(panchangamAdapter);

        updateCurLocation(mainActivity.getLocationCity());

        // Final Step: update Header with today's date in native format (Gregorian format)
        TextView textView = root.findViewById(R.id.nithya_panchangam_hdr);
        int currYear = currCalendar.get(Calendar.YEAR);
        int currDate = currCalendar.get(Calendar.DATE);
        String npHeader = refDinaAnkam + ", " + vaasaramStr + "-" + maasamStr + " (" +
                currDate + "-" +
                currCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                        Locale.ENGLISH) + "-" + currYear + ")";
        textView.setText(npHeader);

        updatePanchangamFieldsHeader();
        long pEndTime = System.nanoTime();
        Log.d("PanchangamProfiler","Overall Update Time Taken: " +
                VedicCalendar.getTimeTaken(pStartTime, pEndTime));
    }

    private void updatePanchangamFieldsHeader() {
        panchangamFields = new ArrayList<>();
        panchangamFields.add(getString(R.string.festivals_events));
        panchangamFields.add(getString(R.string.samvatsaram_heading));
        panchangamFields.add(getString(R.string.aayanam_heading));
        panchangamFields.add(getString(R.string.rithou_heading));
        panchangamFields.add(getString(R.string.maasam_heading));
        panchangamFields.add(getString(R.string.paksham_heading));
        panchangamFields.add(getString(R.string.thithi_heading));
        panchangamFields.add(getString(R.string.vaasaram_heading));
        panchangamFields.add(getString(R.string.raasi_heading));
        panchangamFields.add(getString(R.string.nakshatram_heading));
        panchangamFields.add(getString(R.string.chandrashtama_nakshathram_heading));
        panchangamFields.add(getString(R.string.lagnam_heading));
        panchangamFields.add(getString(R.string.horai_heading));
        panchangamFields.add(getString(R.string.yogam_heading));
        panchangamFields.add(getString(R.string.karanam_heading));
        panchangamFields.add(getString(R.string.amruthathi_yogam_heading));
        panchangamFields.add(getString(R.string.sunrise_heading));
        panchangamFields.add(getString(R.string.sunset_heading));
        panchangamFields.add(getString(R.string.good_time_heading));
        panchangamFields.add(getString(R.string.raahu_kaalam_heading));
        panchangamFields.add(getString(R.string.emakandam_heading));
        panchangamFields.add(getString(R.string.kuligai_heading));
        panchangamFields.add("");
    }
}