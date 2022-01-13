package com.gkmhc.vedanta.nithya_panchangam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.gkmhc.utils.VedicCalendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Use this Calendar activity to display monthly calendar (as per Drik panchangam).
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class NithyaPanchangamCalendar extends AppCompatActivity implements
        CalendarAdapter.OnItemListener {
    private Calendar calendar;
    private VedicCalendar vedicCalendar;
    private int npYear;
    private int refYear;
    private int npMonth;
    private int refMonth;
    private int npDate;
    private int refDate;
    private int selPosition = -1;
    private String selLocale;
    private TextView monthYearText;
    private RecyclerView calendarRecyclerView;
    private static final int npCalendarRequestCode = 2102;
    private ArrayList<String> gregDaysInMonth = null;
    private ArrayList<String> drikDaysInMonth = null;
    private ArrayList<List<Integer>> drikImgIDOfMonth = null;
    private ArrayList<String> drikDinam = null;
    private ArrayList<String> drikDinaVishesham = null;
    private ArrayList<String> drikMaasam = null;
    private ArrayList<String> drikNatchathiram = null;
    private String[] gregYearList = null;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        selLocale = MainActivity.updateSelLocale(this);
        Locale locale = new Locale(selLocale);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        HashMap<Integer, String[]> vedicCalendarLocaleList = MainActivity.buildVedicCalendarLocaleList(this);
        String location = MainActivity.readDefLocationSetting(getApplicationContext());
        int ayanamsaMode = MainActivity.readPrefAyanamsaSelection(getApplicationContext());
        MainActivity.PlacesInfo placesInfo = MainActivity.getLocationDetails(location);
        int panchangamType = MainActivity.readPrefPanchangamType(this);
        try {
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
            getSupportActionBar().setLogo(R.mipmap.ic_launcher_round);
            getSupportActionBar().setBackgroundDrawable(
                    ResourcesCompat.getDrawable(getResources(), R.drawable.default_background, null));
            setAppTitle();

            calendar = Calendar.getInstance();
            npYear = calendar.get(Calendar.YEAR);
            refYear = npYear;
            npMonth = calendar.get(Calendar.MONTH);
            refMonth = npMonth;
            npDate = calendar.get(Calendar.DATE);
            refDate = npDate;
            vedicCalendar = VedicCalendar.getInstance(
                    MainActivity.getPathToLocalAssets(getApplicationContext()),
                    panchangamType, calendar, placesInfo.longitude, placesInfo.latitude,
                    placesInfo.timezone, ayanamsaMode, MainActivity.readPrefChaandramanaType(this),
                    vedicCalendarLocaleList);
        } catch (Exception e) {
            // Nothing to do here.
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nithya_panchangam_calendar);
        this.getWindow().getDecorView().setBackgroundColor(
                getResources().getColor(android.R.color.holo_red_dark));
        initWidgets();
        setMonthView();

        formYearList();
        TextView npCalendarTitle = findViewById(R.id.np_calendar_title);
        npCalendarTitle.setPaintFlags(npCalendarTitle.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        npCalendarTitle.setOnClickListener(v -> showYearsDialog());

        ImageView prevYearView = findViewById(R.id.np_calendar_prevYear);
        prevYearView.setOnClickListener(v -> {
            calendar.set(npYear, npMonth, 1);
            calendar.add(Calendar.YEAR, -1);
            npYear = calendar.get(Calendar.YEAR);
            setMonthView();
            if ((npMonth != refMonth) || (npYear != refYear)) {
                selPosition = -1;
            }
            updateVisbilityForMenuItem(false);
        });

        ImageView prevMonthView = findViewById(R.id.np_calendar_prevMonth);
        prevMonthView.setOnClickListener(v -> {
            calendar.set(npYear, npMonth, 1);
            calendar.add(Calendar.MONTH, -1);
            npMonth = calendar.get(Calendar.MONTH);
            npYear = calendar.get(Calendar.YEAR);
            setMonthView();
            if ((npMonth != refMonth) || (npYear != refYear)) {
                selPosition = -1;
            }
            updateVisbilityForMenuItem(false);
        });

        ImageView nextYearView = findViewById(R.id.np_calendar_nextYear);
        nextYearView.setOnClickListener(v -> {
            if ((panchangamType == VedicCalendar.PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
                (panchangamType == VedicCalendar.PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
                // Vakyam calendar shall be revealed every Mar of the year for the next year
                // so that there is no financial trouble for Vakyam calendar makers.
                if (npYear == refYear) {
                    if (npMonth < 3) {
                        Toast.makeText(this, R.string.vakyam_calendar_unavailable,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else if (npYear == (refYear - 1)) {
                    if (npMonth > 2) {
                        Toast.makeText(this, R.string.vakyam_calendar_unavailable,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            calendar.set(npYear, npMonth, 1);
            calendar.add(Calendar.YEAR, 1);
            npYear = calendar.get(Calendar.YEAR);
            setMonthView();
            if ((npMonth != refMonth) || (npYear != refYear)) {
                selPosition = -1;
            }
            updateVisbilityForMenuItem(false);
        });

        ImageView nextMonthView = findViewById(R.id.np_calendar_nextMonth);
        nextMonthView.setOnClickListener(v -> {
            if ((panchangamType == VedicCalendar.PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR) ||
                (panchangamType == VedicCalendar.PANCHANGAM_TYPE_VAKHYAM_LUNAR)) {
                // Vakyam calendar shall be revealed every Mar of the year for the next year
                // so that there is no financial trouble for Vakyam calendar makers.
                // Three Scenarios
                // 1) Current year: 2021, current month: Apr to Dec, Action: Next Month ==> Allow
                // 2) Current year: 2022, current month: Jan, Action: Next Month ==> Allow
                // 3) Current year: 2022, current month: Mar, Action: Next Month ==> Deny
                if ((npYear == refYear) || (npYear == (refYear + 1))) {
                    // 3) Current year: 2022, current month: Mar, Action: Next Month ==> Deny
                    if (npMonth == 2) {
                        Toast.makeText(this, R.string.vakyam_calendar_unavailable,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            calendar.set(npYear, npMonth, 1);
            calendar.add(Calendar.MONTH, 1);
            npMonth = calendar.get(Calendar.MONTH);
            npYear = calendar.get(Calendar.YEAR);
            setMonthView();
            if ((npMonth != refMonth) || (npYear != refYear)) {
                selPosition = -1;
            }
            updateVisbilityForMenuItem(false);
        });
    }

    private void formYearList() {
        int startRange = refYear - 100;
        int endRange = refYear + 100;
        gregYearList = new String[endRange - startRange];
        for (int index = startRange;index < endRange;index++) {
            gregYearList[(index - startRange)] = String.valueOf(index);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.confirm_selection, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.confirm_button) {
            confirmDateChange();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initWidgets() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        monthYearText = findViewById(R.id.np_calendar_title);
    }

    private void setMonthView() {
        monthYearText.setText(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                              Locale.ENGLISH) + " " + npYear);
        selPosition = getDrikDaysInMonth(calendar);
        CalendarAdapter calendarAdapter = new CalendarAdapter(this, gregDaysInMonth,
                drikDaysInMonth, drikImgIDOfMonth, drikMaasam, this);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 7);
        calendarRecyclerView.setHasFixedSize(true);
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);
        calendarAdapter.notifyDataSetChanged();

        selPosition += (refDate - 1);
        new Handler().postDelayed(() -> selectCell(selPosition),100);
    }

    private int getDrikDaysInMonth (Calendar calendar) {
        gregDaysInMonth = new ArrayList<>();
        drikDaysInMonth = new ArrayList<>();
        drikImgIDOfMonth = new ArrayList<>();
        drikDinam = new ArrayList<>();
        drikDinaVishesham = new ArrayList<>();
        drikMaasam = new ArrayList<>();
        drikNatchathiram = new ArrayList<>();

        int numDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar calendarIter = (Calendar) calendar.clone();
        calendarIter.set(npYear, npMonth, 1);
        int firstDate = (calendarIter.get(Calendar.DAY_OF_WEEK) - 1);
        long dStartTime = System.nanoTime();
        for (int index = 0; index < 42; index++) {
            //long mStartTime = System.nanoTime();
            if ((index < firstDate) || (index >= (numDaysInMonth + firstDate))) {
                gregDaysInMonth.add("");
                drikDaysInMonth.add("");
                drikImgIDOfMonth.add(null);
                drikDinam.add("");
                drikDinaVishesham.add("");
                drikMaasam.add("");
                drikNatchathiram.add("");
            } else {
                gregDaysInMonth.add(String.valueOf(index - firstDate + 1));

                //long startTime = System.nanoTime();
                try {
                    //long endTime = System.nanoTime();
                    //Log.d("NPCalProfiler","VedicCalendar()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));

                    // 1) Add Dinaankham
                    //startTime = endTime;
                    vedicCalendar.setCalendarDate((index - firstDate + 1), npMonth, npYear, 0, 0);
                    //endTime = System.nanoTime();
                    //Log.d("NPCalProfiler","setDate()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));
                    //startTime = endTime;
                    int dinaAnkham =
                            vedicCalendar.getDinaAnkam();
                    //endTime = System.nanoTime();
                    //Log.d("NPCalProfiler","getDinaAnkam()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));
                    drikDaysInMonth.add(String.valueOf(dinaAnkham));

                    // 2) Get Thithi
                    //startTime = endTime;
                    String strThithi =
                            vedicCalendar.getTithi(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
                    //endTime = System.nanoTime();
                    //Log.d("NPCalProfiler","getTithi()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));
                    drikDinam.add(strThithi);

                    // 3) Get Maasam
                    //startTime = endTime;
                    String strMaasam =
                            vedicCalendar.getMaasam(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
                    //endTime = System.nanoTime();
                    //Log.d("NPCalProfiler","getMaasam()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));
                    drikMaasam.add(strMaasam);

                    // 4) Get Natchathiram
                    //startTime = endTime;
                    String strNakshatram =
                            vedicCalendar.getNakshatram(VedicCalendar.MATCH_PANCHANGAM_FULLDAY);
                    //endTime = System.nanoTime();
                    //Log.d("NPCalProfiler","getNakshatram()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));
                    drikNatchathiram.add(strNakshatram);

                    // 5) Get a list of dina vishesham(s)
                    //    Add list of strings & icons associated with each vishesham for the given
                    //    calendar day.
                    //startTime = endTime;
                    List<Integer> dinaVisheshamCodeList = vedicCalendar.getDinaVisheshams();
                    List<String> dinaVisheshamStrList = new ArrayList<>();
                    List<Integer> dinaVisheshamImgList = new ArrayList<>();
                    for (int code = 0; code < dinaVisheshamCodeList.size(); code++) {
                        int visheshamCode = dinaVisheshamCodeList.get(code);
                        int labelID = Reminder.getDinaVisheshamLabel(visheshamCode);
                        int iconID = Reminder.getDinaVisheshamImg(visheshamCode);
                        dinaVisheshamStrList.add(getString(labelID));
                        dinaVisheshamImgList.add(iconID);
                    }
                    drikImgIDOfMonth.add(dinaVisheshamImgList);
                    String dinaSpecialStr = dinaVisheshamStrList.toString();
                    dinaSpecialStr = dinaSpecialStr.substring(1, dinaSpecialStr.length() - 1);
                    drikDinaVishesham.add(dinaSpecialStr);
                    //endTime = System.nanoTime();
                    //Log.d("NPCalProfiler","getDinaVisheshams()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));
                    calendarIter.add(Calendar.DATE, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //long mEndTime = System.nanoTime();
            //Log.d("NPCalProfiler","Iter[" + index + "]" + " Time Taken: " +
            //        VedicCalendar.getTimeTaken(mStartTime, mEndTime));
        }
        long dEndTime = System.nanoTime();
        Log.d("NPCalProfiler","getDrikDaysInMonth()... Time Taken: " +
                VedicCalendar.getTimeTaken(dStartTime, dEndTime));

        return firstDate;
    }

    public void confirmDateChange() {
        Intent retIntent = new Intent();
        retIntent.putExtra("Request_Code", npCalendarRequestCode);
        retIntent.putExtra("Calendar_Year", npYear);
        retIntent.putExtra("Calendar_Month", npMonth);
        retIntent.putExtra("Calendar_Date", npDate);
        setResult(RESULT_OK, retIntent);
        finish();
    }

    @Override
    public void onItemClick(View view, int position) {
        String dayText = gregDaysInMonth.get(position);
        if (!dayText.equals("")) {
            if ((selPosition != -1) && (selPosition != position)) {
                selectCell (selPosition);
            }

            if (selPosition != position) {
                view.setSelected(true);
            }
            selPosition = position;
            updateVisbilityForMenuItem(true);
            LayoutInflater layoutInflater = LayoutInflater.from(this);

            npDate = Integer.parseInt(dayText);
            Calendar calendar = Calendar.getInstance();
            calendar.set(npYear, npMonth, npDate);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            String[] arrayList = getResources().getStringArray(R.array.dhinam_list);
            String strVaasaram = arrayList[dayOfWeek - 1];

            Locale locale = new Locale(selLocale);
            Locale.setDefault(locale);
            Resources resources = getResources();
            Configuration configuration = resources.getConfiguration();
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            View npCalendarCellView =
                    layoutInflater.inflate(R.layout.np_calendar_cell_dialog, null);
            TextView textView = npCalendarCellView.findViewById(R.id.np_date_val);
            String dayVal = drikDaysInMonth.get(position) + ", " + strVaasaram + "-" +
                    drikMaasam.get(position) +
                    " (" +
                        npDate + "-" + calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                        Locale.ENGLISH) + "-" + npYear +
                    ")";
            textView.setText(dayVal);
            textView = npCalendarCellView.findViewById(R.id.dina_vishesham);
            textView.setText(drikDinaVishesham.get(position));
            textView = npCalendarCellView.findViewById(R.id.np_cell_maasam);
            textView.setText(drikMaasam.get(position));
            textView = npCalendarCellView.findViewById(R.id.np_cell_thithi);
            textView.setText(drikDinam.get(position));
            textView = npCalendarCellView.findViewById(R.id.np_cell_natchathiram);
            textView.setText(drikNatchathiram.get(position));

            // Set Image Icon(s)
            // Currently 3 dina visheshams(i.e icons) are supported.
            List<Integer> resID = drikImgIDOfMonth.get(position);
            if (resID != null) {
                ImageView imageView1 = npCalendarCellView.findViewById(R.id.np_calendar_icon1);
                ImageView imageView2 = npCalendarCellView.findViewById(R.id.np_calendar_icon2);
                ImageView imageView3 = npCalendarCellView.findViewById(R.id.np_calendar_icon3);
                ImageView imageView4 = npCalendarCellView.findViewById(R.id.np_calendar_icon4);
                if (resID.size() > 0) {
                    imageView1.setImageResource(resID.get(0));
                }

                if (resID.size() == 4) {
                    // Load 3 more icons if dina visheshams is 3
                    imageView2.setImageResource(resID.get(1));
                    imageView3.setImageResource(resID.get(2));
                    imageView4.setImageResource(resID.get(3));
                } else if (resID.size() == 3) {
                    // Load 2 more icons if dina visheshams is 3
                    imageView2.setImageResource(resID.get(1));
                    imageView3.setImageResource(resID.get(2));
                    imageView4.setImageResource(resID.get(2));
                } else if (resID.size() == 2) {
                    // Load 2nd icon for 2nd imageview if dina visheshams is 2
                    imageView2.setImageResource(resID.get(1));

                    // Load 2nd icon for 3rd imageview as well as this may show empty icon
                    // during flipping!
                    imageView3.setImageResource(resID.get(1));
                    imageView4.setImageResource(resID.get(0));
                }

                if (resID.size() > 1) {
                    ViewFlipper viewFlipper = npCalendarCellView.findViewById(R.id.drikCalendarFlipImg);
                    viewFlipper.startFlipping();
                }
            }

            AlertDialog alertDialog =
                    new AlertDialog.Builder(this).setView(npCalendarCellView).create();
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.holo_red_dark);
            alertDialog.show();
        }
    }

    private void selectCell (int position) {
        try {
            View view = Objects.requireNonNull(calendarRecyclerView.getLayoutManager()).findViewByPosition(position);
            if (view != null) {
                view.setSelected(!view.isSelected());
            }
        } catch (Exception e) {
            // Do Nothing!
        }
    }

    private void updateVisbilityForMenuItem (boolean visibility) {
        if (menu != null) {
            menu.getItem(0).setVisible(visibility);
        }
    }

    private void showYearsDialog () {
        AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        dlg.setTitle(getString(R.string.select_year));
        dlg.setCancelable(true);
        dlg.setSingleChoiceItems(gregYearList, 100, (dialog, which) -> {
            dialog.dismiss();
            npYear = Integer.parseInt(gregYearList[which]);
            setMonthView();
            if ((npMonth != refMonth) || (npYear != refYear)) {
                selPosition = -1;
            }
            updateVisbilityForMenuItem(false);
        }).show();
    }

    private void setAppTitle() {
        String strTitle = getString(R.string.thiru_ganitha_panchangam);
        String defPanchangamTypeStr = getString(R.string.pref_def_panchangam);
        SharedPreferences localPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (localPreferences != null) {
            defPanchangamTypeStr = localPreferences.getString(SettingsFragment.PREF_PANCHANGAM_KEY,
                    defPanchangamTypeStr);

            if (defPanchangamTypeStr.equalsIgnoreCase(
                    getString(R.string.pref_panchangam_tamil_vakyam))) {
                strTitle = getString(R.string.vakhya_panchangam);
            } else if (defPanchangamTypeStr.equalsIgnoreCase(
                    getString(R.string.pref_panchangam_drik_telugu_lunar))) {
                strTitle = getString(R.string.telugu_panchangam);
            } else if (defPanchangamTypeStr.equalsIgnoreCase(
                    getString(R.string.pref_panchangam_drik_kannada_lunar))) {
                strTitle = getString(R.string.kannada_panchangam);
            }
        }

        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml(
                "<font color='#0000FF'>" + strTitle + "</font>"));
    }
}