package com.gkmhc.vedanta.nithya_panchangam;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.gkmhc.utils.VedicCalendar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

/**
 * Sankalpam fragment that retrieves all sankalpam details for given day & displays the same.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class Sankalpam extends Fragment {
    private MainActivity mainActivity;
    private Calendar selectedCalendar;
    private View root;
    private TextView begSankalpamTextView;

    private static final int SANKALPAM_TEXT_MAX_SIZE = 140;
    private static final int SANKALPAM_TEXT_MIN_SIZE = 40;
    private static final int SANKALPAM_INCREMENT_SIZE = 4;

    private int mBaseDistZoomIn;
    private int mBaseDistZoomOut;

    private String prefSankalpamType;
    private String samvatsaramStr;
    private String ayanamStr;
    private String rithouStr;
    private String maasamStr;
    private String pakshamStr;
    private String thithiStr;
    private int refDinaAnkam = 0;
    private String vaasaramStr;
    private String natchathiramStr;
    private String yogamStr;
    private String karanamStr;
    private double curLocationLongitude = 0; // Default to Varanasi
    private double curLocationLatitude = 0; // Default to Varanasi
    private static final String PREF_SANKALPAM_TYPE_KEY = "PREF_SANKALPAM_TYPE_KEY";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        prefSankalpamType = getPrefSankalpamType(getContext());
        root = inflater.inflate(R.layout.fragment_sankalpam, container, false);

        mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.updateAppLocale();
            curLocationLongitude = mainActivity.getLongitude();
            curLocationLatitude = mainActivity.getLatitude();
            Log.d("Sankalpam","Longitude: " + curLocationLongitude +
                    " Latitude: " + curLocationLatitude);
        }

        // Handle Prev Date Button
        // Get Prev Date panchangam & update fragment
        FloatingActionButton faBtnDayPrev = root.findViewById(R.id.dayPrev);
        faBtnDayPrev.setOnClickListener(v -> {
            Calendar calendar = mainActivity.getSelectedCalendar();
            calendar.add(Calendar.DATE, -1);
            int refDate = calendar.get(Calendar.DATE);
            int refMonth = calendar.get(Calendar.MONTH);
            int refYear = calendar.get(Calendar.YEAR);
            mainActivity.setSelectedCalendar(refDate, refMonth, refYear);

            refreshSankalpam(true);
            mainActivity.refreshTab(NPAdapter.NP_TAB_PANCHANGAM);
        });

        // Handle Next Date Button
        // Get Next Date panchangam & update fragment
        FloatingActionButton faBtnDayNext = root.findViewById(R.id.dayNext);
        faBtnDayNext.setOnClickListener(v -> {
            Calendar calendar = mainActivity.getSelectedCalendar();
            calendar.add(Calendar.DATE, +1);
            int refDate = calendar.get(Calendar.DATE);
            int refMonth = calendar.get(Calendar.MONTH);
            int refYear = calendar.get(Calendar.YEAR);
            mainActivity.setSelectedCalendar(refDate, refMonth, refYear);

            refreshSankalpam(true);
            mainActivity.refreshTab(NPAdapter.NP_TAB_PANCHANGAM);
        });

        begSankalpamTextView = root.findViewById(R.id.sankalpam_res_begin);
        //begSankalpamTextView.setTextSize(ratio + 15);

        // Inflate the layout for this fragment
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            refreshSankalpam(false);
        } catch (Exception exception) {
            Log.e("Sankalpam:","Exception --- onResume()!");
        }
    }

    /**
     * Use this utility function to update the sankalpam details for the given Calendar date.
     *
     * @param forceRefresh True to force refresh, false to just update partially.
     */
    public void refreshSankalpam(boolean forceRefresh) {
        new Handler().postDelayed(() -> {
            try {
                // code runs in a thread
                boolean toRefresh;
                Calendar calendar = mainActivity.getSelectedCalendar();
                if (forceRefresh) {
                    toRefresh = true;
                } else {
                    if ((selectedCalendar != null) &&
                        (calendar.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR)) &&
                        (calendar.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH)) &&
                        (calendar.get(Calendar.DATE) == selectedCalendar.get(Calendar.DATE))) {
                        toRefresh = false;
                    } else {
                        toRefresh = true;
                    }
                }

                // Refresh completely if set to true, else just update UI.
                String selLocale = mainActivity.updateAppLocale();
                if (toRefresh) {
                    selectedCalendar = calendar;
                    prefSankalpamType = getPrefSankalpamType(getContext());
                    curLocationLongitude = mainActivity.getLongitude();
                    curLocationLatitude = mainActivity.getLatitude();
                    //Log.d("Sankalpam","Longitude: " + curLocationLongitude +
                    //        " Latitude: " + curLocationLatitude);
                    retrieveTodaysPanchangam(selectedCalendar, selLocale);
                }
                updateSankalpamFragment(root, selectedCalendar, prefSankalpamType, selLocale);
            } catch (final Exception ex) {
                Log.e("Sankalpam","Exception in refreshPanchangam()");
            }
        }, 50);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }

    /**
     *  Utility function to retrieve all the values for Vedic Panchangam elements and
     *  store them as data members.
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
        HashMap<String, String[]> vedicCalendarLocaleList =
                MainActivity.buildVedicCalendarLocaleList(getContext());
        String location = MainActivity.readDefLocationSetting(getContext());
        int ayanamsaMode = MainActivity.readPrefAyanamsaSelection(getContext());
        VedicCalendar vedicCalendar = VedicCalendar.getInstance(
                VedicCalendar.PANCHANGAM_TYPE_DRIK_GANITHAM, currCalendar, curLocationLongitude,
                curLocationLatitude, MainActivity.getLocationTimeZone(location), ayanamsaMode,
                vedicCalendarLocaleList);
        if (vedicCalendar != null) {
            // Step1: Calculate Samvatsaram
            samvatsaramStr = vedicCalendar.getSamvatsaram();

            // Step2: Retrieve correct Ayanam given current system time
            ayanamStr = vedicCalendar.getAyanam(VedicCalendar.MATCH_SANKALPAM_EXACT);

            // Step3: Retrieve correct rithou  given current system time
            // Step5: Retrieve correct paksham  given current system time
            // Step6: Retrieve correct thithi  given current system time
            refDinaAnkam = vedicCalendar.getDinaAnkam(VedicCalendar.MATCH_SANKALPAM_EXACT);

            // Step3: Retrieve correct rithou  given current system time
            rithouStr = vedicCalendar.getRithu(VedicCalendar.MATCH_SANKALPAM_EXACT);

            // Step4: Retrieve correct maasam given current system time
            maasamStr = vedicCalendar.getMaasam(VedicCalendar.MATCH_SANKALPAM_EXACT);

            // Step5: Retrieve correct paksham  given current system time
            pakshamStr = vedicCalendar.getPaksham();

            // Step6: Retrieve correct thithi  given current system time
            thithiStr = vedicCalendar.getThithi(VedicCalendar.MATCH_SANKALPAM_EXACT);

            // Step7: Retrieve correct vaasaram for the current thithi
            vaasaramStr = vedicCalendar.getVaasaram(VedicCalendar.MATCH_SANKALPAM_EXACT);

            // Step8: Retrieve correct natchathiram for the current thithi
            natchathiramStr =
                    vedicCalendar.getNakshatram(VedicCalendar.MATCH_SANKALPAM_EXACT);

            // Step9: Retrieve correct Yogam for the current thithi
            yogamStr = vedicCalendar.getYogam(VedicCalendar.MATCH_SANKALPAM_EXACT);

            // Step10: Retrieve correct Karanam for the current thithi
            karanamStr = vedicCalendar.getKaranam(VedicCalendar.MATCH_SANKALPAM_EXACT);
        }
        long pEndTime = System.nanoTime();
        Log.d("Sankalpam:","Overall Retrieve Time Taken: " +
                VedicCalendar.getTimeTaken(pStartTime, pEndTime));
    }

    /**
     * Utility function to update all elements in Sankalpam fragment with all the values retrieved
     * from vedicCalendar mapped to the given Calendar date.
     *  @param root              Root View
     * @param currCalendar      A Calendar date as per Gregorian Calendar
     * @param prefSankalpamType Sankalpam Type (Shubham or Srardham)
     * @param selLocale         Language
*        Note: Currently only 3 locales are supported. (English, Tamil, Sanskrit)
*              If an unsupported locale is given as input, then by default "English" is
     */
    private void updateSankalpamFragment(View root, Calendar currCalendar, String prefSankalpamType,
                                         String selLocale) {
        // Sankalpam involves the following parts:
        // Step 1: Beginning part that involves common constructs as per sankalpam type
        // Step 2: Middle part that are specific to the location, region, space, time etc
        // Step 3: Final part that involves common constructs as per sankalpam type
        long pStartTime = System.nanoTime();
        int currYear = currCalendar.get(Calendar.YEAR);
        int currDate = currCalendar.get(Calendar.DATE);

        // Step 1: Beginning part that involves common constructs as per sankalpam type
        begSankalpamTextView = root.findViewById(R.id.sankalpam_res_begin);
        String sankalpamStr = getString(R.string.sankalpam_shubam_begin) + " ... ";
        if (prefSankalpamType.equals(getString(R.string.pref_sankalpam_type_srardham))) {
            sankalpamStr = getString(R.string.sankalpam_srardham_begin) + " ... ";
        }
        begSankalpamTextView.setOnTouchListener((v, event) -> {
            TextView view = (TextView) v;
            view.bringToFront();
            viewTransformation(view, event);
            return true;
        });

        String sanskritAyanamStr = ayanamStr;
        String sanskritRithouStr = rithouStr;
        String sanskritPakshamStr = pakshamStr;
        String sanskritThithiStr = thithiStr;
        String sanskritVaasaramStr = vaasaramStr + getString(R.string.vaasaram_suffix);
        String sanskritNatchathiramStr = natchathiramStr;
        String sanskritYogamStr = yogamStr;
        String htmlFontHdrStart = "<font color='#990000'>";
        String htmlFontHdrEnd = "</font>";

        // Change last 1 or 2 alphabets in the suffix of below strings as per the grammar of the
        // given locale.
        // Also, remove the "visargam" from strings for sankalpam.
        if (selLocale.equalsIgnoreCase("en")) {
            sanskritAyanamStr = ayanamStr.substring(0, ayanamStr.length() - 2) + "e";
            sanskritRithouStr = rithouStr.substring(0, rithouStr.length() - 1) + "au";
            sanskritPakshamStr = pakshamStr + "pakshe";
        } else if (selLocale.equalsIgnoreCase("ta")) {
            sanskritAyanamStr = ayanamStr.substring(0, ayanamStr.length() - 3) + "னே";
            sanskritRithouStr = rithouStr.substring(0, rithouStr.length() - 2) + "தெள";
            sanskritPakshamStr = pakshamStr + "பக்ஷே";
        } else if (selLocale.equalsIgnoreCase("sa")) {
            sanskritVaasaramStr = sanskritVaasaramStr.replaceAll("ः", "");
            sanskritNatchathiramStr = sanskritNatchathiramStr.replaceAll("ः", "");
            sanskritYogamStr = sanskritYogamStr.replaceAll("ः", "");
            maasamStr = maasamStr.replaceAll("ः", "");
            karanamStr = karanamStr.replaceAll("ः", "");

            sanskritAyanamStr = ayanamStr.substring(0, ayanamStr.length() - 3) + "णे";
            sanskritRithouStr = rithouStr.substring(0, rithouStr.length() - 3) + "तौ";
            sanskritPakshamStr = pakshamStr + "पक्षे";
        } else if (selLocale.equalsIgnoreCase("hi")) {
            sanskritVaasaramStr = sanskritVaasaramStr.replaceAll("ः", "");
            sanskritNatchathiramStr = sanskritNatchathiramStr.replaceAll("ः", "");
            sanskritYogamStr = sanskritYogamStr.replaceAll("ः", "");
            maasamStr = maasamStr.replaceAll("ः", "");
            karanamStr = karanamStr.replaceAll("ः", "");

            sanskritAyanamStr = ayanamStr.substring(0, ayanamStr.length() - 3) + "णे";
            sanskritRithouStr = rithouStr.substring(0, rithouStr.length() - 3) + "तौ";
            sanskritPakshamStr = pakshamStr + "पक्षे";
        } else if (selLocale.equalsIgnoreCase("inc")) {
            sanskritVaasaramStr = sanskritVaasaramStr.replaceAll("ḥ", "");
            sanskritNatchathiramStr = sanskritNatchathiramStr.replaceAll("ḥ", "");
            sanskritYogamStr = sanskritYogamStr.replaceAll("ḥ", "");
            maasamStr = maasamStr.replaceAll("ḥ", "");
            karanamStr = karanamStr.replaceAll("ḥ", "");

            sanskritAyanamStr = ayanamStr.substring(0, ayanamStr.length() - 2) + "ē";
            sanskritRithouStr = rithouStr.substring(0, rithouStr.length() - 2) + "au";
            sanskritPakshamStr = pakshamStr + "pakṣē";
        } else if (selLocale.equalsIgnoreCase("te")) {
            sanskritVaasaramStr = sanskritVaasaramStr.replaceAll("ః", "");
            sanskritNatchathiramStr = sanskritNatchathiramStr.replaceAll("ః", "");
            sanskritYogamStr = sanskritYogamStr.replaceAll("ః", "");
            maasamStr = maasamStr.replaceAll("ః", "");
            karanamStr = karanamStr.replaceAll("ః", "");

            sanskritAyanamStr = ayanamStr.substring(0, ayanamStr.length() - 3) + "ణే";
            sanskritRithouStr = rithouStr.substring(0, rithouStr.length() - 3) + "థౌ";
            sanskritPakshamStr = pakshamStr + "పక్షే";
        } else if (selLocale.equalsIgnoreCase("kn")) {
            sanskritVaasaramStr = sanskritVaasaramStr.replaceAll("ಃ", "");
            sanskritNatchathiramStr = sanskritNatchathiramStr.replaceAll("ಃ", "");
            sanskritYogamStr = sanskritYogamStr.replaceAll("ಃ", "");
            maasamStr = maasamStr.replaceAll("ಃ", "");
            karanamStr = karanamStr.replaceAll("ಃ", "");

            sanskritAyanamStr = ayanamStr.substring(0, ayanamStr.length() - 3) + "ಣೇ";
            sanskritRithouStr = rithouStr.substring(0, rithouStr.length() - 3) + "ತೌ";
            sanskritPakshamStr = pakshamStr + "ಪಕ್ಷೆ";
        } else if (selLocale.equalsIgnoreCase("ml")) {
            sanskritVaasaramStr = sanskritVaasaramStr.replaceAll("ഃ", "");
            sanskritNatchathiramStr = sanskritNatchathiramStr.replaceAll("ഃ", "");
            sanskritYogamStr = sanskritYogamStr.replaceAll("ഃ", "");
            maasamStr = maasamStr.replaceAll("ഃ", "");
            karanamStr = karanamStr.replaceAll("ഃ", "");

            sanskritAyanamStr = ayanamStr.substring(0, ayanamStr.length() - 2) + "നേ";
            sanskritRithouStr = rithouStr.substring(0, rithouStr.length() - 2) + "തൗ";
            sanskritPakshamStr = pakshamStr + "പക്ഷെ";
        }

        // Step 2: Middle part that are specific to the location, region, space, time etc
        // Update only in case of success
        // updateFlag will be false in case of exception path
        String sankalpamMiddle = "<br><br>" +
                htmlFontHdrStart + samvatsaramStr + htmlFontHdrEnd +
                        " " + getString(R.string.sankalpam_nama_samvathsare) + " " +
                        htmlFontHdrStart + sanskritAyanamStr + htmlFontHdrEnd + " " +
                        htmlFontHdrStart + sanskritRithouStr + htmlFontHdrEnd + " " +
                        htmlFontHdrStart + maasamStr + htmlFontHdrEnd + " " +
                        getString(R.string.sankalpam_maase) + " " +
                        htmlFontHdrStart + sanskritPakshamStr + htmlFontHdrEnd + " " +
                        htmlFontHdrStart + sanskritThithiStr + htmlFontHdrEnd + " ";
        if (prefSankalpamType.equals(getString(R.string.pref_sankalpam_type_shubam))) {
            sankalpamMiddle += " " + getString(R.string.sankalpam_subha_thithou) + " ";
        } else if (prefSankalpamType.equals(getString(R.string.pref_sankalpam_type_srardham))) {
            sankalpamMiddle += " " + getString(R.string.sankalpam_punya_thithou) + " ";
        }
        sankalpamMiddle +=
                        htmlFontHdrStart + sanskritVaasaramStr + htmlFontHdrEnd +
                        " " + getString(R.string.sankalpam_yukhtayaam) + " " +
                        htmlFontHdrStart + sanskritNatchathiramStr + htmlFontHdrEnd +
                        " " + getString(R.string.sankalpam_nakshatra_yukhtayaam);
        if (prefSankalpamType.equals(getString(R.string.pref_sankalpam_type_shubam))) {
            sankalpamMiddle += " " + htmlFontHdrStart + sanskritYogamStr +
                    htmlFontHdrEnd + " " + getString(R.string.sankalpam_yoga_yukhtayaam) + " " +
                    htmlFontHdrStart + karanamStr + htmlFontHdrEnd +
                    " " + getString(R.string.sankalpam_karana_yukhtayaam);
        }
        sankalpamStr += sankalpamMiddle + " ... <br><br>";

        // Step 3: Final part that involves common constructs as per sankalpam type
        // If Subham (or) Srardham, then generate sankalpam accordingly.
        if (prefSankalpamType.equals(getString(R.string.pref_sankalpam_type_shubam))) {
            sankalpamStr += getString(R.string.sankalpam_shubam_end) + " ||" + "<br>";
            sankalpamStr = sankalpamStr.replaceAll("\\*\\*\\*\\*",
                    htmlFontHdrStart + sanskritThithiStr + htmlFontHdrEnd);
        } else if (prefSankalpamType.equals(getString(R.string.pref_sankalpam_type_srardham))) {
            sankalpamStr += getString(R.string.sankalpam_srardham_end_1) + " " +
                    htmlFontHdrStart + sanskritThithiStr + htmlFontHdrEnd + " " +
                    getString(R.string.sankalpam_punya_thithou) + " ";
            sankalpamStr += getString(R.string.sankalpam_srardham_end_2) + " " +
                    htmlFontHdrStart + sanskritThithiStr + htmlFontHdrEnd + " " +
                    getString(R.string.sankalpam_srardham_end_3) + " ||" + "<br>";
        }
        //Log.d("Sankalpam", "Value: " + Html.fromHtml(sankalpamStr));
        begSankalpamTextView.setText(Html.fromHtml(sankalpamStr));

        // Final Step: update Header with today's date in native format (Gregorian format)
        TextView hdrTextView = root.findViewById(R.id.sankalpam_hdr);
        String npHeader = refDinaAnkam + ", " + vaasaramStr + "-" + maasamStr + " (" +
                currDate + "-" +
                currCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                        Locale.ENGLISH) + "-" + currYear + ")";
        hdrTextView.setText(npHeader);
        long pEndTime = System.nanoTime();
        Log.d("Sankalpam:","Overall Update Time Taken: " +
                VedicCalendar.getTimeTaken(pStartTime, pEndTime));
    }

    /**
     * Utility function to get the selected sankalpam type from shared preferences.
     *
     * @return Selected sankalpam type as a string
     */
    private String getPrefSankalpamType (Context context) {
        String defSankalpamType = getString(R.string.pref_sankalpam_type_shubam);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null) {
            defSankalpamType = preferences.getString(PREF_SANKALPAM_TYPE_KEY, defSankalpamType);
        }
        return defSankalpamType;
    }

    /**
     * Utility function to handle text zoom functionality.
     */
    private void viewTransformation(View view, MotionEvent event) {
        if (event.getPointerCount() == 2) {
            TextView viewById = (TextView) view;
            int action = event.getAction();
            int pure = action & MotionEvent.ACTION_MASK;

            if (pure == MotionEvent.ACTION_POINTER_DOWN
                    && viewById.getTextSize() <= SANKALPAM_TEXT_MAX_SIZE
                    && viewById.getTextSize() >= SANKALPAM_TEXT_MIN_SIZE) {
                mBaseDistZoomIn = getDistanceFromEvent(event);
                mBaseDistZoomOut = getDistanceFromEvent(event);
            } else {
                int currentDistance = getDistanceFromEvent(event);
                if (currentDistance > mBaseDistZoomIn) {
                    float finalSize = viewById.getTextSize() + SANKALPAM_INCREMENT_SIZE;
                    if (finalSize > SANKALPAM_TEXT_MAX_SIZE) {
                        finalSize = SANKALPAM_TEXT_MAX_SIZE;
                    }
                    viewById.setTextSize(TypedValue.COMPLEX_UNIT_PX, finalSize);
                } else {
                    if (currentDistance < mBaseDistZoomOut) {
                        float finalSize = viewById.getTextSize() - SANKALPAM_INCREMENT_SIZE;
                        if (finalSize < SANKALPAM_TEXT_MIN_SIZE) {
                            finalSize = SANKALPAM_TEXT_MIN_SIZE;
                        }
                        viewById.setTextSize(TypedValue.COMPLEX_UNIT_PX, finalSize);
                    }
                }
            }
        }
    }

    // Utility function to get the distance between the multiple touch
    int getDistanceFromEvent(MotionEvent event) {
        int dx = (int) (event.getX(0) - event.getX(1));
        int dy = (int) (event.getY(0) - event.getY(1));
        return (int) (Math.sqrt(dx * dx + dy * dy));
    }
}