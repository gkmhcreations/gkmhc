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
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
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
import android.widget.ViewFlipper;

import com.gkmhc.utils.VedicCalendar;

import java.util.ArrayList;
import java.util.Calendar;
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
    private int npYear;
    private int refYear;
    private int npMonth;
    private int refMonth;
    private int npDate;
    private int refDate;
    private int selPosition = -1;
    private String sellocale;
    private TextView monthYearText;
    private RecyclerView calendarRecyclerView;
    private static final int npCalendarRequestCode = 2102;
    private ArrayList<String> gregDaysInMonth = null;
    private ArrayList<String> drikDaysInMonth = null;
    private ArrayList<List<Integer>> drikImgIDOfMonth = null;
    private ArrayList<String> drikDhinam = null;
    private ArrayList<String> drikDhinaVishesham = null;
    private ArrayList<String> drikMaasam = null;
    private ArrayList<String> drikNatchathiram = null;
    private String[] gregYearList = null;
    private double curLocationLongitude = 0; // Default to Varanasi
    private double curLocationLatitude = 0; // Default to Varanasi
    private static final String PREF_NP_LOCALE_KEY = "PREF_NP_LOCALE_KEY";
    public static final String PREF_LOCATION_DEF_VAL_KEY = "PREF_LOCATION_DEF_VAL_KEY";
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            String prefLang = sharedPreferences.getString(PREF_NP_LOCALE_KEY, "");
            sellocale = prefLang.substring(0,2);
        } catch (Exception e) {
            // Fallback to default language preference
            sellocale = "en";
        }
        Locale locale = new Locale(sellocale);
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        configuration.setLocale(locale);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            getApplicationContext().createConfigurationContext(configuration);
        } else {
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
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

        String defLocation = sharedPreferences.getString(PREF_LOCATION_DEF_VAL_KEY, "");
        if (defLocation.isEmpty()) {
            defLocation = getString(R.string.pref_def_location_val);
        }
        getLocationCoords(defLocation);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nithya_panchangam_calendar);
        this.getWindow().getDecorView().setBackgroundColor(
                getResources().getColor(android.R.color.holo_red_dark));
        initWidgets();

        calendar = Calendar.getInstance();
        npYear = calendar.get(Calendar.YEAR);
        refYear = npYear;
        npMonth = calendar.get(Calendar.MONTH);
        refMonth = npMonth;
        npDate = calendar.get(Calendar.DATE);
        refDate = npDate;
        setMonthView();

        formYearList();
        TextView npCalendarTitle = findViewById(R.id.np_calendar_title);
        npCalendarTitle.setOnClickListener(v -> showYearsDialog());

        ImageView prevYearView = findViewById(R.id.np_calendar_prevYear);
        prevYearView.setOnClickListener(v -> {
            calendar.set(npYear, npMonth, npDate);
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
            calendar.set(npYear, npMonth, npDate);
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
            calendar.set(npYear, npMonth, npDate);
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
            calendar.set(npYear, npMonth, npDate);
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

    public void getLocationCoords(String locationStr) {
        //String place = (String)adapterView.getItemAtPosition(position);

        try {
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
        }
    }

    private void setMonthView() {
        monthYearText.setText(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,
                              Locale.ENGLISH) + " " + npYear);
        selPosition = getDrikDaysInMonth(calendar);
        CalendarAdapter calendarAdapter = new CalendarAdapter(gregDaysInMonth, drikDaysInMonth,
                drikImgIDOfMonth, this);
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
        drikDhinam = new ArrayList<>();
        drikDhinaVishesham = new ArrayList<>();
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
                drikDhinam.add("");
                drikDhinaVishesham.add("");
                drikMaasam.add("");
                drikNatchathiram.add("");
            } else {
                gregDaysInMonth.add(String.valueOf(index - firstDate + 1));

                //long startTime = System.nanoTime();
                String location = MainActivity.readDefLocationSetting(getApplicationContext());
                VedicCalendar vedicCalendar = VedicCalendar.getInstance(
                        VedicCalendar.PANCHANGAM_TYPE_DRIK_GANITHAM, calendarIter,
                        curLocationLongitude, curLocationLatitude,
                        MainActivity.getLocationTimeZone(location));
                if (vedicCalendar != null) {
                    //long endTime = System.nanoTime();
                    //Log.d("NPCalendar","VedicCalendar()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));

                    // 1) Get Thithi
                    //startTime = System.nanoTime();
                    String strMsg =
                            vedicCalendar.getThithi(sellocale, VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
                    //endTime = System.nanoTime();
                    //Log.d("NPCalendar","getThithi()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));
                    drikDhinam.add(strMsg);

                    // 2) Add Dhinaankham
                    //startTime = System.nanoTime();
                    int dhinaAnkham =
                            vedicCalendar.getDhinaAnkham(VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
                    //endTime = System.nanoTime();
                    //Log.d("NPCalendar","getDinaankham()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));
                    drikDaysInMonth.add(String.valueOf(dhinaAnkham));

                    // 3) Get Maasam
                    //startTime = System.nanoTime();
                    strMsg =
                            vedicCalendar.getMaasam(sellocale, VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
                    //endTime = System.nanoTime();
                    //Log.d("NPCalendar","getMaasam()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));
                    drikMaasam.add(strMsg);

                    // 4) Get Natchathiram
                    //startTime = System.nanoTime();
                    strMsg =
                            vedicCalendar.getNakshatram(sellocale, VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
                    //endTime = System.nanoTime();
                    //Log.d("NPCalendar","getNatchathiram()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));
                    drikNatchathiram.add(strMsg);

                    // 5) Get a list of dhina vishesham(s)
                    //    Add list of strings & icons associated with each vishesham for the given
                    //    calendar day.
                    //startTime = System.nanoTime();
                    List<Integer> dhinaVisheshamCodeList =
                            vedicCalendar.whatIsSpecialToday(VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
                    List<String> dhinaVisheshamStrList = new ArrayList<>();
                    List<Integer> dhinaVisheshamImgList = new ArrayList<>();
                    for (int code = 0;code < dhinaVisheshamCodeList.size();code++) {
                        int visheshamCode = dhinaVisheshamCodeList.get(code);
                        int labelID = Reminder.getDhinaVisheshamLabel(visheshamCode);
                        int iconID = Reminder.getDhinaVisheshamImg(visheshamCode);
                        dhinaVisheshamStrList.add(getString(labelID));
                        dhinaVisheshamImgList.add(iconID);
                    }
                    drikImgIDOfMonth.add(dhinaVisheshamImgList);
                    String dhinaSpecialStr = dhinaVisheshamStrList.toString();
                    dhinaSpecialStr = dhinaSpecialStr.substring(1, dhinaSpecialStr.length() - 1);
                    drikDhinaVishesham.add(dhinaSpecialStr);
                    //endTime = System.nanoTime();
                    //Log.d("NPCalendar","whatIsSpecialToday()... Time Taken: " +
                    //        VedicCalendar.getTimeTaken(startTime, endTime));
                    calendarIter.add(Calendar.DATE, 1);
                }
            }
            //long mEndTime = System.nanoTime();
            //Log.d("NPCalendar","Iter[" + index + "]" + " Time Taken: " +
            //        VedicCalendar.getTimeTaken(mStartTime, mEndTime));
        }
        long dEndTime = System.nanoTime();
        Log.d("NPCalendar","getDrikDaysInMonth()... Time Taken: " +
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
            String strVaasaram = VedicCalendar.getVaasaram(calendar, sellocale,
                                                           VedicCalendar.MATCH_PANCHANGAM_PROMINENT);

            Locale locale = new Locale(sellocale);
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
            textView = npCalendarCellView.findViewById(R.id.dhina_vishesham);
            textView.setText(drikDhinaVishesham.get(position));
            textView = npCalendarCellView.findViewById(R.id.np_cell_maasam);
            textView.setText(drikMaasam.get(position));
            textView = npCalendarCellView.findViewById(R.id.np_cell_thithi);
            textView.setText(drikDhinam.get(position));
            textView = npCalendarCellView.findViewById(R.id.np_cell_natchathiram);
            textView.setText(drikNatchathiram.get(position));

            // Set Image Icon(s)
            // Currently 3 dhina visheshams(i.e icons) are supported.
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
                    // Load 3 more icons if dhina visheshams is 3
                    imageView2.setImageResource(resID.get(1));
                    imageView3.setImageResource(resID.get(2));
                    imageView4.setImageResource(resID.get(3));
                } else if (resID.size() == 3) {
                    // Load 2 more icons if dhina visheshams is 3
                    imageView2.setImageResource(resID.get(1));
                    imageView3.setImageResource(resID.get(2));
                    imageView4.setImageResource(resID.get(2));
                } else if (resID.size() == 2) {
                    // Load 2nd icon for 2nd imageview if dhina visheshams is 2
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
        View view = calendarRecyclerView.getLayoutManager().findViewByPosition(position);
        if (view != null) {
            view.setSelected(!view.isSelected());
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
}