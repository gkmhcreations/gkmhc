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
    private VedicCalendar vedicCalendar;
    private View root;
    private ListView panchangamListView;
    private ArrayList<String> panchangamFields;
    private ArrayList<String> panchangamValues;
    private static int NUM_PANCHANGAM_FIELDS = 27;
    private String maasamStr;
    private int refDinaAnkam = 0;
    private String vaasaramStr;
    private ArrayList<VedicCalendar.KaalamInfo> lagnamExactList;
    private ArrayList<VedicCalendar.KaalamInfo> lagnamFullDayList;
    private ArrayList<VedicCalendar.KaalamInfo> kaalamExactList;
    private ArrayList<VedicCalendar.KaalamInfo> kaalamVibhaagahList;
    private ArrayList<VedicCalendar.KaalamInfo> horaiExactList;
    private ArrayList<VedicCalendar.KaalamInfo> horaiFullDayList;
    private String curLocationCity = "";
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
            vedicCalendar.add(Calendar.DATE, -1);
            refreshPanchangam(true);
            mainActivity.refreshTab(NPAdapter.NP_TAB_SANKALPAM);
        });

        // Handle Next Date Button
        // Get Next Date panchangam & update fragment
        FloatingActionButton faBtnDayNext = root.findViewById(R.id.npDayNext);
        faBtnDayNext.setOnClickListener(v -> {
            vedicCalendar.add(Calendar.DATE, 1);
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
                    mainActivity.initVedicCalendar();
                    refreshPanchangam(true);

                    // Refresh Sankalpam as well when there is a location change.
                    mainActivity.refreshTab(NPAdapter.NP_TAB_SANKALPAM);
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
            exception.printStackTrace();
        }
    }

    /**
     * Use this utility function to update the Panchangam details for the given Calendar date.
     */
    public void refreshPanchangam(boolean forceRefresh) {
        new Handler().postDelayed(() -> {
            try {
                // code runs in a thread
                boolean toRefresh = false;
                VedicCalendar vedicCalendarTemp = null;

                /*
                 * Sometimes, this function may be called while Main Activity is initializing.
                 * So, check before continuing.
                 */
                if (mainActivity != null) {
                    vedicCalendarTemp = mainActivity.getVedicCalendar();
                    mainActivity.updateAppLocale();
                } else {
                    return;
                }

                /*
                 * Refresh Panchangam only when there is a change in one or all of the following
                 * settings:
                 * 1) Locale change
                 * 2) Location change
                 * 3) Panchangam type change
                 * 4) Date change
                 * 5) Chaandramaanam change
                 *
                 * Note: No need to refresh when user is shifting between tabs!
                 */
                if (!forceRefresh) {
                    if (vedicCalendar != null) {
                        int tempDate = vedicCalendarTemp.get(Calendar.DATE);
                        int tempMonth = vedicCalendarTemp.get(Calendar.MONTH);
                        int tempYear = vedicCalendarTemp.get(Calendar.YEAR);

                        int date = vedicCalendar.get(Calendar.DATE);
                        int month = vedicCalendar.get(Calendar.MONTH);
                        int year = vedicCalendar.get(Calendar.YEAR);

                        // Refresh when one of the following occurs:
                        // 1) vedicCalendar instance has changed
                        // 2) Either date or month or year or all have changed
                        if ((vedicCalendarTemp != vedicCalendar) ||
                            (tempDate != date) || (tempMonth != month) || (tempYear != year)) {
                            toRefresh = true;
                        }
                    } else {
                        // Refresh when during first-time load (vedicCalendar will be null)
                        toRefresh = true;
                    }
                } else {
                    // Refresh when caller forces to refresh!
                    toRefresh = true;
                }

                // Retrieve the panchangam fields all over again!
                if (toRefresh) {
                    vedicCalendar = vedicCalendarTemp;
                    retrieveTodaysPanchangam();
                }
                updatePanchangamFieldsHeader();
                updatePanchangamFragment(root);
            } catch (final Exception ex) {
                ex.printStackTrace();
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
     */
    private void retrieveTodaysPanchangam() {
        // Create a VedicCalendar instance.
        // 1) Current Calendar - Today or any given date
        // 2) localpath - Path to SwissEph conf files
        // 3) Longitude - This is important as panchangam calculations changes with location
        // 4) Latitude - This is important as panchangam calculations changes with location
        long pStartTime = System.nanoTime();
        //long startTime = System.nanoTime();

        /*
         * Don't fetch details in case VedicCalendar instance is unavailable / invalid.
         */
        if (vedicCalendar != null) {
            //long endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","VedicCalendar()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));
            panchangamValues = new ArrayList<>();

            // Step1: Retrieve Sunrise timings for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getSunrise());
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getSunrise()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step2: Retrieve Sunset timings for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getSunset());
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getSunset()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step3: Retrieve Kaala Vibhagaha timings for the given calendar day
            panchangamValues.add("");
            kaalamExactList = vedicCalendar.getKaalaVibhaagam(VedicCalendar.MATCH_SANKALPAM_EXACT);
            kaalamVibhaagahList =
                    vedicCalendar.getKaalaVibhaagam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);

            // Step4: Retrieve Samvatsaram for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getSamvatsaram(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getSamvatsaram()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step5: Retrieve Ayanam for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getAyanam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getAyanam()... Time Taken: " +
            //      VedicCalendar.getTimeTaken(startTime, endTime));

            // Step6: Retrieve Dina Ankham for the given calendar day
            //startTime = System.nanoTime();
            refDinaAnkam = vedicCalendar.getDinaAnkam();
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getDinaankham()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step7: Retrieve Rithu for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getRithu(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getRithu()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step8-1: Retrieve Sauramaanam Maasam for the given calendar day
            //startTime = System.nanoTime();
            maasamStr = vedicCalendar.getSauramaanamMaasam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
            panchangamValues.add(maasamStr);

            // Step8-2: Retrieve Chaandramaanam Maasam for the given calendar day
            //startTime = System.nanoTime();
            maasamStr = vedicCalendar.getChaandramaanamMaasam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
            panchangamValues.add(maasamStr);

            // For display purposes only!
            maasamStr = vedicCalendar.getMaasam(VedicCalendar.MATCH_SANKALPAM_EXACT);
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getMaasam()... " + maasamStr + " Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step9: Retrieve Paksham for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getPaksham(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getPaksham()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step10-1: Retrieve Tithi for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getTithi(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));

            // Step10-2: Retrieve Shraadha Tithi for the given calendar day
            panchangamValues.add(vedicCalendar.getShraaddhaTithi(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getTithi()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step11: Retrieve Vaasaram for the given calendar day
            //startTime = System.nanoTime();
            vaasaramStr = vedicCalendar.getVaasaram(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
            panchangamValues.add(vaasaramStr);
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getVaasaram()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step12: Retrieve Raasi for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getRaasi(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getRaasi()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step13: Retrieve Nakshatram for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(
                    vedicCalendar.getNakshatram(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getNatchathiram()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step14: Retrieve Chandrashtama Nakshatram for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(
                    vedicCalendar.getChandrashtamaNakshatram(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getChandrashtamaNatchathiram()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step15: Retrieve Yogam for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getYogam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getYogam()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step16: Retrieve Karanam for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getKaranam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getKaranam()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step17: Retrieve Lagnam list for the given calendar day
            //startTime = System.nanoTime();
            lagnamExactList = vedicCalendar.getLagnam(VedicCalendar.MATCH_SANKALPAM_EXACT);
            panchangamValues.add("");
            lagnamFullDayList =
                    vedicCalendar.getLagnam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);

            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getLagnam()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step18: Retrieve Horai List for the given calendar day
            //startTime = System.nanoTime();
            horaiExactList = vedicCalendar.getHorai(VedicCalendar.MATCH_SANKALPAM_EXACT);
            panchangamValues.add("");
            horaiFullDayList =
                    vedicCalendar.getHorai(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getHorai()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step19: Retrieve Amruthathi Yogam for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(
                    vedicCalendar.getAmruthathiYogam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getAmruthathiYogam()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step20: Retrieve Shubha Kaalam for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getShubhaKaalam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getNallaNeram()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step21: Retrieve Raahu Kaalam Timings for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getRaahuKaalamTimings(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getRaahuKaalamTimings()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step22: Retrieve Yamagandam Timings for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getYamakandamTimings(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getYamakandamTimings()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            // Step23: Retrieve Kuligai Timings for the given calendar day
            //startTime = System.nanoTime();
            panchangamValues.add(vedicCalendar.getKuligaiTimings(VedicCalendar.MATCH_PANCHANGAM_FULLDAY));
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getKuligaiTimings()... Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));

            //startTime = System.nanoTime();
            List<Integer> dhinaVisheshamCodeList = vedicCalendar.getDinaVisheshams();
            String dhinaSpecialStr;
            if (dhinaVisheshamCodeList.size() > 0) {
                // Form a list of Dina Vishesham(s) in case there is more than 1 occuring in a
                // given calendar day.
                List<String> dhinaVisheshamStrList = new ArrayList<>();
                for (int code = 0; code < dhinaVisheshamCodeList.size(); code++) {
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
            panchangamValues.add(vedicCalendar.getShoolamParihaaram());
            panchangamValues.add("");
            //endTime = System.nanoTime();
            //Log.d("PanchangamProfiler","getDinaVishesham()..." + " Time Taken: " +
            //        VedicCalendar.getTimeTaken(startTime, endTime));
        }

        long pEndTime = System.nanoTime();
        Log.d("PanchangamProfiler", "Overall Retrieve Time Taken: " +
                VedicCalendar.getTimeTaken(pStartTime, pEndTime));
    }

    /**
     * Utility function to update all elements in Panchangam fragment with all the values
     * retrieved from vedicCalendar mapped to the given Calendar date.
     *
     * @param root  Root View
     */
    private void updatePanchangamFragment(View root) {
        long pStartTime = System.nanoTime();
        int pos = panchangamListView.getFirstVisiblePosition();
        int numPanchangamFields = panchangamValues.size();
        if (numPanchangamFields == NUM_PANCHANGAM_FIELDS) {
            PanchangamAdapter panchangamAdapter = new PanchangamAdapter(getContext(),
                    panchangamFields, panchangamValues, lagnamExactList, lagnamFullDayList,
                    kaalamExactList, kaalamVibhaagahList, horaiExactList, horaiFullDayList);
            panchangamListView.setAdapter(panchangamAdapter);
            panchangamListView.setSelection(pos);
        }

        updateCurLocation(mainActivity.getLocationCity());

        // Final Step: update Header with today's date in native format (Gregorian format)
        TextView textView = root.findViewById(R.id.nithya_panchangam_hdr);
        int currYear = vedicCalendar.get(Calendar.YEAR);
        int currDate = vedicCalendar.get(Calendar.DATE);
        int dayOfWeek = vedicCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        String npHeader = refDinaAnkam + ", " + vaasaramStr + "-" + maasamStr + " (" +
                dayNames[dayOfWeek] + ", " + currDate + "-" +
                vedicCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                        Locale.ENGLISH) + "-" + currYear + ")";
        textView.setText(npHeader);

        updatePanchangamFieldsHeader();
        long pEndTime = System.nanoTime();
        Log.d("PanchangamProfiler","Overall Update Time Taken: " +
                VedicCalendar.getTimeTaken(pStartTime, pEndTime));
    }

    private void updatePanchangamFieldsHeader() {
        // Need to reconstruct everytime as this can change as per locale selected!
        panchangamFields = new ArrayList<>();
        panchangamFields.add(getString(R.string.festivals_events));
        panchangamFields.add(getString(R.string.sunrise_heading));
        panchangamFields.add(getString(R.string.sunset_heading));
        panchangamFields.add(getString(R.string.kaala_vibhaagaha));
        panchangamFields.add(getString(R.string.samvatsaram_heading));
        panchangamFields.add(getString(R.string.aayanam_heading));
        panchangamFields.add(getString(R.string.rithou_heading));
        panchangamFields.add(getString(R.string.sauramanam_maasam_heading));
        panchangamFields.add(getString(R.string.chaandra_maasam_heading));
        panchangamFields.add(getString(R.string.paksham_heading));
        panchangamFields.add(getString(R.string.tithi_heading));
        panchangamFields.add(getString(R.string.shraadha_tithi_heading));
        panchangamFields.add(getString(R.string.vaasaram_heading));
        panchangamFields.add(getString(R.string.raasi_heading));
        panchangamFields.add(getString(R.string.nakshatram_heading));
        panchangamFields.add(getString(R.string.chandrashtama_nakshathram_heading));
        panchangamFields.add(getString(R.string.yogam_heading));
        panchangamFields.add(getString(R.string.karanam_heading));
        panchangamFields.add(getString(R.string.lagnam_heading));
        panchangamFields.add(getString(R.string.horai_heading));
        panchangamFields.add(getString(R.string.amruthathi_yogam_heading));
        panchangamFields.add(getString(R.string.good_time_heading));
        panchangamFields.add(getString(R.string.raahu_kaalam_heading));
        panchangamFields.add(getString(R.string.emakandam_heading));
        panchangamFields.add(getString(R.string.kuligai_heading));
        panchangamFields.add(getString(R.string.shoolam_heading));
        panchangamFields.add("");
    }
}