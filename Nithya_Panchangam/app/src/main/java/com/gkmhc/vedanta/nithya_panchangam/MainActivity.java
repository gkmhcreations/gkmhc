package com.gkmhc.vedanta.nithya_panchangam;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.gkmhc.utils.CopyToAssets;
import com.gkmhc.utils.VedicCalendar;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.tabs.TabLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Main Activity of Nithya Panchangam Android App.
 * MainActivity.java - This file contains all App-specific common handling.
 *
 * Following features are supported in this App:
 * A) Main Features represented via 5 tabs:
 *   1) Panchangam
 *      - Panchangam.java - This file contains the functions that handle panchangam fragment.
 *   2) Sankalpam
 *      - Sankalpam.java - This file contains the functions that handle Sankalpam fragment.
 *   3) Alarm
 *      - Alarm.java            - This file contains functions that are associated with Alarms
 *                                functions like Start, Stop, Restart and also to handle the UI.
 *      - AlarmAdapter.java     - This file contains functions that displays Alarms in a ListView
 *      - AlarmViewHolder.java  - A view holder that holds Alarm information
 *      - HandleAlarmReminderActivity.java - A common activity to add/modify Alarms and Reminders.
 *   4) Reminder
 *      - Reminder.java           - This file contains functions that are associated with Reminder
 *                                  functions like Start, Stop, Restart and also to handle the UI.
 *      - ReminderAdapter.java    - This file contains functions that displays Reminders in a ListView.
 *      - ReminderViewHolder.java - A view holder that holds Reminder information
 *   5) StopWatch
 *      - StopWatch.java - This file contains the functions that handle StopWatch fragment.
 *
 * B) Widget
 *    - NithyaPanchangamWidget.java - This file contains functions that handle widget for the App.
 *
 * C) Settings
 *   Nithya Panchangam allows the following settings to be configured by user
 *   - Locale               - Sanskrit / Tamil / English
 *   - Panchangam Type      - "Drik Ganitham", "Tamil Vakyam", "Telugu" & "Kannada" are supported.
 *   - Location Type        - Manual / GPS
 *   - Sankalpam Type       - Shubham / Srartham
 *   All of these are handled in below files:
 *   - NithyaPanchangamSettings - This file contains the functions that handle Settings Activity
 *                                to read/write Nithya Panchangam settings information to/from a
 *                                persistent storage (SharedPreferences).
 *   - SettingsFragment.java    - This file contains the functions that handle Settings fragment.
 *
 * D) Support/Utility Functions
 *   1) Vedic Calendar
 *      - VedicCalendar.java - This file contains the generic functions that does all the
 *                             Panchangam & Sankalpam calculations and exposes a simple-to-use
 *                             interface(API) for the Apps.
 *                             SwissEph library is used for all solar & lunar calculations.
 *                             Note: This is reusable across different platforms.
 *   2) Broadcast Receiver
 *      - NPBroadcastReceiver.java - This file contains functions that handles all lifecycles
 *                                   event related to Alarms & Reminders.
 *   3) Adapter
 *      - NPAdapter.java - This file contains functions that handle create/display information
 *                         related to all the 5 tabs in this App.
 *   4) NPDB
 *      - NPDB.java - This file contains generic interfaces to read/write Alarms & Reminders
 *                    to/from a persistent storage(SQLite).
 *   5) Lock Screen Notification
 *      - AlarmLockScreenNotification.java - This file display a fullscreen Activity when an Alarm
 *                                           fires but Phone's display is locked.
 *   6) Monthly Calendar
 *      - NithyaPanchangamCalendar.java - This file contains functions that displays the Monthly
 *                                        calendar and handles day/month/year selections as well.
 *      - CalendarAdapter.java          - This file contains functions that handle each cell in a
 *                                        calendar in the form of a RecyclerView.
 *      - CalendarViewHolder.java       - A view holder that holds "Dhina" information
 *   7) Splash
 *      - SplashScreen.java     - This file contains simple functions to load an activity
 *                                at the start of NithyaPanchangam Activity launch.
 *   8) About
 *      - AboutActivity.java    - This file contains functions to display information about the App.
 *   9) Copy SwissEph Asset Files
 *      - CopyToAssets.java   - This file contains functions that handle copying SwissEph related
 *                                metadata information to the App's default location on the Phone.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class MainActivity extends AppCompatActivity implements LocationListener {
    private VedicCalendar vedicCalendar;
    private SharedPreferences sharedPreferences;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private NPAdapter myAdapter;
    private int selTabPos = 0;
    private int prefLocationType = LOCATION_MANUAL;
    private int prefAyanamsaType = VedicCalendar.AYANAMSA_CHITRAPAKSHA;
    private int prefChaandramanamType = VedicCalendar.CHAANDRAMAANAM_TYPE_AMANTA;
    private String prefPanchangamVal = "";
    private int prefTimeFormat = VedicCalendar.PANCHANGAM_TIME_FORMAT_HHMM;
    private static String prefSankalpamType = "";
    private static String selLocale = "en";
    private static String curLocationCity = "";
    private static final double INDIAN_STANDARD_TIME = 5.5;
    public static final String DINA_VISHESHAM_RULES_FILE = "nithya_panchangam.toml";
    public static final String NP_UPDATE_WIDGET = "Nithya_Panchangam_Update_Widget";
    public static final String NP_CHANNEL_ID = "Nithya_Panchangam";
    public static final int LOCATION_MANUAL = 0;
    public static final int LOCATION_GPS = 1;

    public static final String TIMEZONE_INDIA = "Asia/Kolkata";
    public static final String TIMEZONE_ASIA = "Asia";
    public static final String TIMEZONE_AUSTRALIA = "Australia";
    public static final String TIMEZONE_AFRICA = "Africa";
    public static final String TIMEZONE_AMERICA = "America";
    public static final String TIMEZONE_EUROPE = "Europe";

    private static final int SETTINGS_REQUEST_CODE = 2100;
    private static final int ABOUT_REQUEST_CODE = 2101;
    private static final int CALENDAR_REQUEST_CODE = 2102;
    private static final int LOCATION_UPDATE_REQUEST_CODE = 2103;
    private static final int REQUEST_PERMISSIONS_CODE = 2104;
    private static final int RAASI_CHART_CODE = 2105;
    private static final int KAALA_VIBHAGAM_CODE = 2106;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private double curLocationLongitude = 0; // Default to Varanasi
    private double curLocationLatitude = 0; // Default to Varanasi
    private static final HashMap<String, PlacesInfo> placesTimezoneDB = new HashMap<>();
    public static List<String> placesList;
    private static HashMap<Integer, String[]> vedicCalendarLocaleList = new HashMap<>();

    public static class PlacesInfo {
        public final double longitude;
        public final double latitude;
        public final String timeZoneID;

        PlacesInfo(double longitude, double latitude, String timeZoneID) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.timeZoneID = timeZoneID;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Copy NP Assets to right directory for VedicCalendar's use.
        new CopyToAssets(".*?\\.(se1|txt|xml|toml?)", getApplicationContext()).copy();

        createNotificationChannel();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        buildPlacesTimezoneDB();

        curLocationCity = readDefLocationSetting(getApplicationContext());
        refreshLocation();

        // Step 1: Get the preferred locale from preferences and update activity.
        prefSankalpamType = readPrefSankalpamType(this);
        prefLocationType = readPrefLocationSelection();
        prefAyanamsaType = readPrefAyanamsaSelection(this);
        prefChaandramanamType = readPrefChaandramanaType(this);
        prefPanchangamVal = readPrefPanchangam(this);
        prefTimeFormat = readPrefTimeFormat(this);
        updateAppLocale();
        setAppTitle();

        // Step 2: Sort out the toolbar icon & logos
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher_round);
        getSupportActionBar().setBackgroundDrawable(
                ResourcesCompat.getDrawable(getResources(), R.drawable.default_background, null));

        initVedicCalendar();

        // Step 4: Create required tabs via adapter and attach them to viewpager
        tabLayout = findViewById(R.id.npTabs);
        viewPager = findViewById(R.id.view_pager);
        myAdapter = new NPAdapter(this, getSupportFragmentManager());
        myAdapter.notifyDataSetChanged();
        viewPager.setAdapter(myAdapter);
        for (int index = 0; index < myAdapter.getCount(); index += 1) {
            switch (index) {
                case NPAdapter.NP_TAB_PANCHANGAM:
                    tabLayout.addTab(tabLayout.newTab().setText(R.string.panchangam_tab_heading));
                    break;
                case NPAdapter.NP_TAB_SANKALPAM:
                    tabLayout.addTab(tabLayout.newTab().setText(R.string.sankalpam_tab_heading));
                    break;
                case NPAdapter.NP_TAB_ALARM:
                    tabLayout.addTab(tabLayout.newTab().setText(R.string.alarm_tab_heading));
                    break;
                case NPAdapter.NP_TAB_REMINDER:
                    tabLayout.addTab(tabLayout.newTab().setText(R.string.reminder_tab_heading));
                    break;
                case NPAdapter.NP_TAB_STOPWATCH:
                    tabLayout.addTab(tabLayout.newTab().setText(R.string.stopwatch_tab_heading));
                    break;
            }
        }
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selTabPos = tab.getPosition();
                viewPager.setCurrentItem(selTabPos);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(alarmMsgReceiver, new IntentFilter(NPBroadcastReceiver.STOP_ALARM));

        //Log.d("MainActivity","initSwissEph()... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime, endTime));
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

    // Copy all the SwissEph assets to the local directory.
    // On Android, this can be done in the below suggested way.
    //
    // Note: When using VedicCalendar class on other platforms then all the
    // SwissEph assets needs to be copied to the local directory as per the local platform
    // on VedicCalendar is being used.
    public static String getPathToLocalAssets(Context context) {
        //new CopyToAssets(".*?\\.(se1|xml|toml?)", context).copy();
        return context.getFilesDir() + File.separator + "ephe";
    }

    private final BroadcastReceiver alarmMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase(NPBroadcastReceiver.STOP_ALARM)) {
                boolean alarmType = intent.getBooleanExtra(
                        NPBroadcastReceiver.EXTRA_NOTIFICATION_ALARM_TYPE,
                        Alarm.ALARM_TYPE_STANDARD);

                if (alarmType == Alarm.ALARM_TYPE_STANDARD) {
                    refreshTab(NPAdapter.NP_TAB_ALARM);
                } else if (alarmType == Alarm.ALARM_TYPE_VEDIC) {
                    refreshTab(NPAdapter.NP_TAB_REMINDER);
                }
            }
        }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    private void refreshLocation() {
        updateManualLocation(curLocationCity);
        if (LOCATION_MANUAL == readPrefLocationSelection()) {
            // If Manual option is selected, then provide a way select a location
            // Use geocoder to retrieve {Location, longitude, latitude}
            // Also, stop the updates from network provider.
            LocationManager locationManager =
                    (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(this::onLocationChanged);
        } else {
            // If GPS option is selected, then seek permissions and retrieve
            // {Location, longitude, latitude} from LocationListener
            initGPSLocation();
        }
    }

    public boolean updateManualLocation(String locationStr) {
        try {
            /*
            // TODO - How can we retrieve this automatically without delays?
            // If we use Google Places API, then there is payment associated.
            // Is there any other way?
            List<Address> addressList = geocoder.getFromLocationName(locationStr, 10);
            if ((addressList != null) && (addressList.size() > 0)) {
                String strLocality = addressList.get(0).getLocality();
                curLocationCity = locationStr;
                curLocationLongitude = addressList.get(0).getLongitude();
                curLocationLatitude = addressList.get(0).getLatitude();
                updateDefLocationSetting(curLocationCity);
                Log.d("MainActivity", "Location: " + curLocationCity +
                        " Longitude: " + curLocationLongitude +
                        " Latitude: " + curLocationLatitude);
                if (strLocality == null) {
                    strLocality = curLocationCity;
                }
                Toast.makeText(this, "Location(manual selection): " + strLocality,
                        Toast.LENGTH_SHORT).show();
                return true;
            }*/
            PlacesInfo placesInfo = placesTimezoneDB.get(locationStr);
            if (placesInfo != null) {
                curLocationCity = locationStr;
                curLocationLongitude = placesInfo.longitude;
                curLocationLatitude = placesInfo.latitude;
                updateDefLocationSetting(curLocationCity);
                return true;
            }
        } catch (Exception e) {
            // Nothing to do here.
            Log.d("MainActivity","Exception in initManualLocation()");
        }

        // Set default location in case of failure
        curLocationCity = getString(R.string.pref_def_location_val);
        curLocationLongitude = (82 + 58.34 / 60.0);
        curLocationLatitude = (25 + 19 / 60.0);
        return false;
    }

    private void initGPSLocation() {
        new Thread() {
            @Override
            public void run() {
                try {
                    // code runs in a thread
                    runOnUiThread(() -> {
                        // 1) Get Longitude & Latitude of given location
                        // 2) Get Location City Details (only when user permissions are available)
                        getCurrentCoords();
                    });
                } catch (final Exception ex) {
                    Log.d("MainActivity","Exception in initGPSLocation()");
                }
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(this);

        /*LocationRequest locationRequest = LocationRequest.create();
        //locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000); // 10 seconds
        //locationRequest.setFastestInterval(500); // 5 seconds*/

        /*LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            Log.d("MainActivity", "onLocationResult() --- Longitude: " +
                                    curLocationLongitude + " Lattitude: " + curLocationLatitude);
                            curLocationLatitude = location.getLatitude();
                            curLocationLongitude = location.getLongitude();
                        }
                    }
                }
            }
        };*/
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        boolean retVal = true;
        int itemID = item.getItemId();
        if (itemID == R.id.settings_np) {
            startActivityForResult(new Intent(this, NithyaPanchangamSettings.class),
                    SETTINGS_REQUEST_CODE);
        } else if (itemID == R.id.about_np) {
            startActivityForResult(new Intent(this, AboutActivity.class),
                    ABOUT_REQUEST_CODE);
        } else if (itemID == R.id.learn_more_np) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.gkmhc.in/gkm-heritage-creations/apps-games/nithya-panchangam"));
            startActivity(browserIntent);
        } /*else if (itemID == R.id.raasi_chart) {
            Intent raasiChartIntent = new Intent(this, RaasiChart.class);
            raasiChartIntent.putExtra(RaasiChart.EXTRA_DATE, vedicCalendar.get(Calendar.DATE));
            raasiChartIntent.getIntExtra(RaasiChart.EXTRA_MONTH, vedicCalendar.get(Calendar.MONTH));
            raasiChartIntent.getIntExtra(RaasiChart.EXTRA_YEAR, vedicCalendar.get(Calendar.YEAR));
            startActivityForResult(raasiChartIntent, RAASI_CHART_CODE);
        } */else if (itemID == R.id.calendar_np) {
            // Create a dialog for displaying Calender with "English" as default locale
            Locale locale = new Locale("EN");
            Locale.setDefault(locale);
            Resources resources = getResources();
            Configuration configuration = resources.getConfiguration();
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            startActivityForResult(new Intent(this,
                    NithyaPanchangamCalendar.class), CALENDAR_REQUEST_CODE);
        } else {
            retVal = super.onOptionsItemSelected(item);
        }

        return retVal;
    }

    public void initVedicCalendar() {
        Calendar currCalendar = Calendar.getInstance();
        if (vedicCalendar != null) {
            int date = vedicCalendar.get(Calendar.DATE);
            int month = vedicCalendar.get(Calendar.MONTH);
            int year = vedicCalendar.get(Calendar.YEAR);
            currCalendar.set(year, month, date);
        }

        try {
            String location = readDefLocationSetting(this);
            String assetsLocation = getPathToLocalAssets(this);
            int ayanamsaMode = readPrefAyanamsaSelection(this);
            PlacesInfo placesInfo = getLocationDetails(location);
            vedicCalendarLocaleList.clear();
            vedicCalendarLocaleList = buildVedicCalendarLocaleList(this);
            vedicCalendar = VedicCalendar.getInstance(assetsLocation,
                    readPrefPanchangamType(this), currCalendar, placesInfo.longitude,
                    placesInfo.latitude, placesInfo.timeZoneID, ayanamsaMode,
                    readPrefChaandramanaType(this), vedicCalendarLocaleList);
            int selectedTimeFormat = readPrefTimeFormat(this);
            vedicCalendar.setTimeFormat(selectedTimeFormat);

            // Configure Dina Vishesham Rules
            vedicCalendar.configureDinaVisheshamRules(assetsLocation + "/" + DINA_VISHESHAM_RULES_FILE);
        } catch (Exception e) {
            e.printStackTrace();
            vedicCalendar = null;
        }
    }

    public VedicCalendar getVedicCalendar() {
        return vedicCalendar;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Log.i("MainActivity","Request Code: " + requestCode + " Response Code: " + resultCode);

        // Upon Activity Results, do the following
        // 1) If Activity results is for Settings, then update all fragments with selected locale
        // 2) If Activity results is for Alarm, then create Alarm based on alarm type
        // Ignore the rest
        if (requestCode == SETTINGS_REQUEST_CODE) {
            boolean refreshPanchangam = false;

            // If Manual location has changed, then refresh the tabs
            String selectedLocation = readDefLocationSetting(getApplicationContext());
            if (!selectedLocation.equalsIgnoreCase(curLocationCity)) {
                curLocationCity = selectedLocation;
                refreshLocation();
                refreshPanchangam = true;
            }

            // If there is change in sankalpam preferences, then refresh location & the fragments.
            String sankalpamType = readPrefSankalpamType(this);
            if (!prefSankalpamType.equalsIgnoreCase(sankalpamType)) {
                prefSankalpamType = sankalpamType;
                refreshTab(NPAdapter.NP_TAB_SANKALPAM);
                // No need to inform widget here for Sankalpam change!
            }

            // If there is change in location preferences, then refresh location & the fragments.
            int prefToUpdate = readPrefLocationSelection();
            if (prefLocationType != prefToUpdate) {
                prefLocationType = prefToUpdate;
                refreshLocation();
                refreshPanchangam = true;
            }

            // If there is change in Ayanamsa preferences, then refresh location & the fragments.
            int selectedAyanamsa = readPrefAyanamsaSelection(this);
            if (prefAyanamsaType != selectedAyanamsa) {
                prefAyanamsaType = selectedAyanamsa;
                refreshPanchangam = true;
            }

            // If there is change in Chaandramanam preferences, then refresh location & the fragments.
            int selectedChaandramanamType = readPrefChaandramanaType(this);
            if (prefChaandramanamType != selectedChaandramanamType) {
                prefChaandramanamType = selectedChaandramanamType;
                refreshPanchangam = true;
            }

            // If there is change in Panchangam preferences, then refresh location & the fragments.
            String configuredPanchangam = readPrefPanchangam(this);
            if (!prefPanchangamVal.equalsIgnoreCase(configuredPanchangam)) {
                prefPanchangamVal = configuredPanchangam;
                refreshPanchangam = true;
            }

            // If there is change in Time Format preferences, then refresh panchangam fragment.
            int selectedTimeFormat = readPrefTimeFormat(this);
            if (prefTimeFormat != selectedTimeFormat) {
                prefTimeFormat = selectedTimeFormat;
                refreshPanchangam = true;
            }

            // If there is change in locale preferences, then refresh location & the fragments.
            String defLocale = readPrefLocale();
            if (!defLocale.equalsIgnoreCase(selLocale)) {
                refreshTab(NPAdapter.NP_TAB_ALARM);
                refreshTab(NPAdapter.NP_TAB_REMINDER);
                refreshPanchangam = true;
            }

            if (refreshPanchangam) {
                refreshPanchangamDetails();
                setAppTitle();

                // Send broadcast Intent to widget(s) to refresh!
                sendBroadcastToWidget(this);
            }
        } else if (requestCode == CALENDAR_REQUEST_CODE) {
            if (data != null) {
                int calYear = data.getIntExtra("Calendar_Year", 0);
                int calMonth = data.getIntExtra("Calendar_Month", 0);
                int calDate = data.getIntExtra("Calendar_Date", 0);
                vedicCalendar.setDate(calDate, calMonth, calYear, 0, 0);
                refreshPanchangamDetails();

                // No need to inform widget here for calendar date change!
            }
        }
    }

    private void refreshPanchangamDetails() {
        updateAppLocale();
        updateTabTitle();

        initVedicCalendar();
        refreshTab(NPAdapter.NP_TAB_PANCHANGAM);
        refreshTab(NPAdapter.NP_TAB_SANKALPAM);
    }

    public void refreshTab(int tabPosition) {
        switch (tabPosition) {
            case NPAdapter.NP_TAB_PANCHANGAM:
                Panchangam panchangamFragment = (Panchangam) myAdapter.getItem(tabPosition);
                panchangamFragment.refreshPanchangam(true);
                break;
            case NPAdapter.NP_TAB_SANKALPAM:
                Sankalpam sankalpamFragment = (Sankalpam) myAdapter.getItem(tabPosition);
                sankalpamFragment.refreshSankalpam(true);
                break;
            case NPAdapter.NP_TAB_ALARM:
                Alarm alarmFragment = (Alarm) myAdapter.getItem(tabPosition);
                alarmFragment.refreshAlarms();
                break;
            case NPAdapter.NP_TAB_REMINDER:
                Reminder reminderFragment = (Reminder)myAdapter.getItem(tabPosition);
                reminderFragment.refreshReminders();
                break;
        }
    }

    /**
     * Utility function to get the selected locale from the shared preferences.
     *
     * @return Selected locale as a string
     */
    private String readPrefLocale() {
        String defLocale = "En";
        SharedPreferences localPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        try {
            String prefLang = localPreferences.getString(SettingsFragment.PREF_NP_LOCALE_KEY, "En");
            defLocale = prefLang.substring(0, 2);
        } catch (Exception e) {
            // Fallback to default language preference
        }

        return defLocale;
    }

    /**
     * Utility function to find if App is launched for the first-time.
     *
     * @return true if started first time, false otherwise.
     */
    public boolean isAppLaunchedFirstTime() {
        return sharedPreferences.getBoolean(SettingsFragment.PREF_APP_LAUNCH_FIRST_TIME_KEY, true);
    }

    /**
     * Utility function to update App is launched for the first-time field.
     */
    public void updateAppLaunchedFirstTime() {
        sharedPreferences.edit().putBoolean(SettingsFragment.PREF_APP_LAUNCH_FIRST_TIME_KEY, false).apply();
    }

    /**
     * Utility function to get the selected locale from the shared preferences.
     *
     * @return Selected locale as a string
     */
    private int readPrefLocationSelection() {
        int prefLocationCode = LOCATION_MANUAL;
        String prefLocationStr = sharedPreferences.getString(SettingsFragment.PREF_LOCATION_SETTINGS_KEY, "");
        if (prefLocationStr.equalsIgnoreCase(getString(R.string.pref_location_gps))) {
            prefLocationCode = LOCATION_GPS;
        }

        return prefLocationCode;
    }

    /**
     * Utility function to get the preferred panchangam type from the shared preferences.
     *
     * @return Selected panchangam type as an integer value.
     */
    public static int readPrefPanchangamType(Context context) {
        int defPanchangamType = VedicCalendar.PANCHANGAM_TYPE_DRIK_GANITHAM_LUNI_SOLAR;
        String defPanchangamTypeStr = context.getString(R.string.pref_def_panchangam);
        SharedPreferences localPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (localPreferences != null) {
            defPanchangamTypeStr = localPreferences.getString(SettingsFragment.PREF_PANCHANGAM_KEY,
                                                              defPanchangamTypeStr);
            if (defPanchangamTypeStr.equalsIgnoreCase(
                    context.getString(R.string.pref_panchangam_tamil_vakyam))) {
                defPanchangamType = VedicCalendar.PANCHANGAM_TYPE_VAKHYAM_LUNI_SOLAR;
            } else if (defPanchangamTypeStr.equalsIgnoreCase(
                    context.getString(R.string.pref_panchangam_drik_telugu_lunar))) {
                defPanchangamType = VedicCalendar.PANCHANGAM_TYPE_DRIK_GANITHAM_LUNAR;
            } else if (defPanchangamTypeStr.equalsIgnoreCase(
                    context.getString(R.string.pref_panchangam_drik_kannada_lunar))) {
                defPanchangamType = VedicCalendar.PANCHANGAM_TYPE_DRIK_GANITHAM_LUNAR;
            }
        }

        return defPanchangamType;
    }

    /**
     * Utility function to get the preferred panchangam from the shared preferences.
     *
     * @return Selected panchangam type as a string value.
     */
    public static String readPrefPanchangam(Context context) {
        String defPanchangamTypeStr = context.getString(R.string.pref_def_panchangam);
        SharedPreferences localPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (localPreferences != null) {
            defPanchangamTypeStr = localPreferences.getString(SettingsFragment.PREF_PANCHANGAM_KEY,
                    defPanchangamTypeStr);
        }

        return defPanchangamTypeStr;
    }

    /**
     * Utility function to get the preferred Chaandramana type from the shared preferences.
     *
     * @return Selected chaandramana type as an integer value.
     */
    public static int readPrefChaandramanaType(Context context) {
        int defChaandramanaType = VedicCalendar.CHAANDRAMAANAM_TYPE_AMANTA;
        String defChaandramanaTypeStr = context.getString(R.string.pref_def_lunar_calendar_type);
        SharedPreferences localPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (localPreferences != null) {
            defChaandramanaTypeStr = localPreferences.getString(SettingsFragment.PREF_CHAANDRAMANA_CALENDAR_KEY,
                    defChaandramanaTypeStr);
            if (defChaandramanaTypeStr.equalsIgnoreCase(
                    context.getString(R.string.pref_lunar_calendar_type_purnimanta))) {
                defChaandramanaType = VedicCalendar.CHAANDRAMAANAM_TYPE_PURNIMANTA;
            }
        }

        return defChaandramanaType;
    }

    /**
     * Utility function to get the selected locale from the shared preferences.
     *
     * @return Selected locale as a string
     */
    public static String readPrefSankalpamType(Context context) {
        String defSankalpamType = context.getString(R.string.pref_sankalpam_type_shubam);
        SharedPreferences localPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (localPreferences != null) {
            return localPreferences.getString(SettingsFragment.PREF_SANKALPAM_TYPE_KEY, defSankalpamType);
        }

        return defSankalpamType;
    }

    /**
     * Utility function to get the selected ayanamsa from the shared preferences.
     *
     * @return Selected ayanamsa as a string
     */
    public static int readPrefAyanamsaSelection(Context context) {
        int prefAyanamsa = VedicCalendar.AYANAMSA_CHITRAPAKSHA;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences != null) {
            String prefAyanamsaStr = sharedPreferences.getString(SettingsFragment.PREF_AYANAMSA_KEY, "");
            if (prefAyanamsaStr.equalsIgnoreCase(context.getString(R.string.pref_ayanamsa_lahiri))) {
                prefAyanamsa = VedicCalendar.AYANAMSA_LAHIRI;
            } else if (prefAyanamsaStr.equalsIgnoreCase(context.getString(R.string.pref_ayanamsa_krishnamurti))) {
                prefAyanamsa = VedicCalendar.AYANAMSA_KRISHNAMURTI;
            }
        }

        return prefAyanamsa;
    }

    /**
     * Utility function to get the default preferred location from the shared preferences.
     *
     * @return Selected locale as a string
     */
    public static String readDefLocationSetting(Context context) {
        String location = context.getString(R.string.pref_def_location_val);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null) {
            location = preferences.getString(SettingsFragment.PREF_LOCATION_DEF_VAL_KEY,
                                             context.getString(R.string.pref_def_location_val));
        }

        if (location.isEmpty()) {
            location = context.getString(R.string.pref_def_location_val);
        }
        return location;
    }

    /**
     * Utility function to get the preferred time format from the shared preferences.
     *
     * @return Preferred Timeformat.
     */
    public static int readPrefTimeFormat(Context context) {
        String timeFormat = context.getString(R.string.pref_def_timeformat);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null) {
            timeFormat = preferences.getString(SettingsFragment.PREF_TIMEFORMAT_KEY,
                            context.getString(R.string.pref_def_timeformat));
        }

        if (timeFormat.isEmpty()) {
            timeFormat = context.getString(R.string.pref_def_timeformat);
        }

        if (timeFormat.equalsIgnoreCase(context.getString(R.string.pref_timeformat_hhmm))) {
            return VedicCalendar.PANCHANGAM_TIME_FORMAT_HHMM;
        }
        return VedicCalendar.PANCHANGAM_TIME_FORMAT_NAZHIGAI;
    }

    /**
     * Utility function to update the default preferred location in the shared preferences.
     */
    private void updateDefLocationSetting(String locationToUpdate) {
        sharedPreferences.edit().putString(SettingsFragment.PREF_LOCATION_DEF_VAL_KEY, locationToUpdate).apply();
    }

    /**
     * Fragments use this API to update the locale for all the elements.
     *
     * @return Modified language as a string.
     */
    public static String updateSelLocale(Context context) {
        SharedPreferences localPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (localPreferences != null) {
            String prefLang = localPreferences.getString(SettingsFragment.PREF_NP_LOCALE_KEY, "En");
            try {
                selLocale = getLocaleShortStr(prefLang);
            } catch (Exception e) {
                // Fallback to default language preference
                selLocale = "en";
            }

            Locale locale = new Locale(selLocale);
            Locale.setDefault(locale);
            Resources resources = context.getApplicationContext().getResources();
            Configuration config = resources.getConfiguration();
            config.locale = locale;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }

        return selLocale;
    }

    // Update title for all tabs
    // This is used when locale is changed or when orientation is changed
    private void updateTabTitle() {
        for (int index = 0; index < myAdapter.getCount(); index += 1) {
            Objects.requireNonNull(tabLayout.getTabAt(index)).setText(myAdapter.getPageTitle(index));
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateAppLocale();
        updateTabTitle();
    }

    public String getLocationCity() {
        /*Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
        List<Address> addressList;
        try {
            Log.d("MainActivity", "Get Location City: " + curLocationCity +
                    " Longitude: " + curLocationLongitude +
                    " Latitude: " + curLocationLatitude);
            addressList = geoCoder.getFromLocationName(curLocationCity, 1);
            if (addressList != null) {
                String strLocality = addressList.get(0).getLocality();
                if (strLocality != null) {
                    return strLocality;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        return curLocationCity;
    }

    private void getCurrentCoordsDetails () {
        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            if (fusedLocationProviderClient != null) {
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.d("MainActivity", "getCurrentCoords() location success!");
                        curLocationLongitude = location.getLongitude();
                        curLocationLatitude = location.getLatitude();
                        getCurrentLocation();
                        Log.d("MainActivity", "Current Location --- Longitude: " +
                                curLocationLongitude + " Lattitude: " + curLocationLatitude);
                    }
                });
            } else {
                Log.d("MainActivity",
                        "getCurrentCoords() fusedLocationProviderClient unavailable!");
            }
        }
    }

    /**
     * Utility function to get the longitude & latitude of the current location
     *
     * Scenarios to be handled:
     *   1) If permissions to access location services is already available, then retrieve
     *      longitude & latitude
     *   2) If permissions to access location services is NOT available, then request user
     *      permissions
     *      2.1) If permissions are "Allowed", then retrieve longitude & latitude
     *      2.2) If permissions are "Denied" once, then inform user about of the importance of
     *           location permissions for this App's successful behavior and then STAY QUIET.
     *           App should continue to work properly. Hence, assume a default location!
     *      2.3) If permissions are "Denied" permanently, then inform user about App permissions
     *           settings in case they change their mind in future.
     *           2.3.1) If user updates permissions settings to "Allow" access, then handle this
     *                  change of permissions and then retrieve longitude & latitude
     *           2.3.2) If user takes no action, then also App should continue to work properly.
     *                  Hence, assume a default location!
     *
     */
    public void getCurrentCoords() {
        try {
            Log.d("MainActivity", "getCurrentCoords() called...");

            if ((ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {

                Log.d("MainActivity", "getCurrentCoords() permission granted!");
                // First-time user has allowed location access
                // 1) If permissions to access location services is already available, then retrieve
                //    longitude & latitude
                getCurrentCoordsDetails();
            } else {
                // 2) If permissions to access location services is NOT available, then request
                //    user permissions
                // Permissions Unavailable in any of the following scenarios:
                // 1) Permissions being requested 2nd time after being denied 1st time
                // 2) Permissions have been recently updated to denied state
                Log.d("MainActivity", "getCurrentCoords() permission denied!");

                // Returns True if user might be confused.
                // So, explain rationale for seeking permissions!
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this,
                            "Location permission is critical to calculate Panchangam!",
                            Toast.LENGTH_SHORT).show();
                }
                // Request for permissions in case of a soft denial without "Don't ask again"
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_UPDATE_REQUEST_CODE);
            }
        } catch (Exception e) {
            curLocationCity = readDefLocationSetting(getApplicationContext());
            Toast.makeText(this, "Permissions not granted. Default Location: " + curLocationCity,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_UPDATE_REQUEST_CODE) {
            // 2.1) If permissions are "Allowed", then retrieve longitude & latitude
            if ((grantResults.length > 0) &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission Granted
                Log.d("MainActivity", "onRequestPermissionsResult() permission granted!");
                Toast.makeText(this, "Location Permission Granted! Thanks!",
                        Toast.LENGTH_SHORT).show();
                getCurrentCoordsDetails();
            } else {
                // 2.2) If permissions are "Denied" once, then inform user about of the importance of
                //      location permissions for this App's successful behavior and then STAY QUIET.
                //      App should continue to work properly. Hence, assume a default location!
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Log.d("MainActivity", "onRequestPermissionsResult() permission denied!");
                    Toast.makeText(this, "Location Permission NOT granted!",
                            Toast.LENGTH_SHORT).show();
                    curLocationCity = readDefLocationSetting(getApplicationContext());
                } else {
                    // 2.3) If permissions are "Denied" permanently, then inform user about App permissions
                    //      settings in case they change their mind in future.
                    // 2.3.1) If user updates permissions settings to "Allow" access, then handle this
                    //        change of permissions and then retrieve longitude & latitude
                    // 2.3.2) If user takes no action, then also App should continue to work properly.
                    //        Hence, assume a default location!
                    Log.d("MainActivity",
                            "onRequestPermissionsResult() permission denied permanently!");
                    // First time permission denied.
                    Toast.makeText(this, "Location Permission Denied Permanently!",
                            Toast.LENGTH_SHORT).show();
                    curLocationCity = readDefLocationSetting(getApplicationContext());

                    // Permission denied permanently. Hence, inform user!
                    new AlertDialog.Builder(this)
                            .setMessage("Location permission is critical to calculate Panchangam!!!")
                            .setPositiveButton("Go to Settings", (dialog, which) ->
                                    changeAppSettings())
                            .setNegativeButton("Cancel", null)
                            .setCancelable(false)
                            .show();
                }
            }
        }
    }

    private void getCurrentLocation() {
        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            try {
                LocationManager locationManager =
                        (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        500, 5, this::onLocationChanged);
            } catch (Exception e) {
                // Error handling!
                Log.d("MainActivity", "getCurrentLocation() location unknown!");
                curLocationCity = readDefLocationSetting(getApplicationContext());
                Toast.makeText(this, "Location fetch failed. Default Location: " +
                                curLocationCity, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        try {
            Geocoder geoCoder = new Geocoder(this, Locale.ENGLISH);
            List<Address> addressList = geoCoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1);
            if (addressList != null) {
                String country = addressList.get(0).getCountryName();
                String state = addressList.get(0).getAdminArea();
                curLocationCity = addressList.get(0).getLocality() + ", " + country;
                String pincode = addressList.get(0).getPostalCode();
                String address = addressList.get(0).getAddressLine(0);

                updateDefLocationSetting(curLocationCity);

                // Inform & Refresh Current Fragment!
                refreshPanchangamDetails();
                Toast.makeText(this, "Location(GPS): " + curLocationCity,
                        Toast.LENGTH_SHORT).show();
                Log.i("MainActivity:", "Country: " + country + " State: " + state + " City: " +
                        curLocationCity + " pincode: " + pincode + " address: " + address);
            } else {
                curLocationCity = readDefLocationSetting(getApplicationContext());
                Toast.makeText(this, "Location fetch failed. Default Location: " +
                        curLocationCity, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // Error Handling!
            Log.d("MainActivity", "onLocationChanged() location unknown!");
            curLocationCity = readDefLocationSetting(getApplicationContext());
            Toast.makeText(this, "Location fetch failed. Default Location: " +
                    curLocationCity, Toast.LENGTH_SHORT).show();
        }
    }

    private void changeAppSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, REQUEST_PERMISSIONS_CODE);
    }

    public static PlacesInfo getLocationDetails(String locationStr) {
        return placesTimezoneDB.get(locationStr);
    }

    public static void buildPlacesTimezoneDB() {
        // Build a list of locations manually.
        // TODO - Is there a way, below info can be retrieved across the world?
        //        {city, state, country, longitude, latitude, timezone}
        // Google Places is paid service. Is there another way?
        // If yes, we can use it. Right now, we are populating manually for 1st version of this App.
        // Disadvantages of using manual approach:
        // - Maintenance
        // - Difficult to automatically align to Day light savings (DLS)
        placesTimezoneDB.put("Chennai, India", new PlacesInfo(80.2707, 13.0827, TIMEZONE_INDIA));
        placesTimezoneDB.put("Mumbai, India", new PlacesInfo(72.8777, 19.0760, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kolkata, India", new PlacesInfo(88.3639, 22.5726, TIMEZONE_INDIA));
        placesTimezoneDB.put("New Delhi, India", new PlacesInfo(77.2090, 28.6139, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thirunelveli, India", new PlacesInfo(77.7567, 8.7139, TIMEZONE_INDIA));
        placesTimezoneDB.put("Salem, India", new PlacesInfo(78.1460, 11.6643, TIMEZONE_INDIA));
        placesTimezoneDB.put("Chidambaram, India", new PlacesInfo(79.6912, 11.4070, TIMEZONE_INDIA));
        placesTimezoneDB.put("Tenkasi, India", new PlacesInfo(77.3161, 8.9594, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kumbakonam, India", new PlacesInfo(79.3845, 10.9602, TIMEZONE_INDIA));
        placesTimezoneDB.put("Coimbatore, India", new PlacesInfo(76.9558, 11.0168, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thanjavur, India", new PlacesInfo(79.1378, 10.7870, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kancheepuram, India", new PlacesInfo(79.6947, 12.8185, TIMEZONE_INDIA));
        placesTimezoneDB.put("Erode, India", new PlacesInfo(77.7172, 11.3410, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thiruvannamalai, India", new PlacesInfo(79.0747, 12.2253, TIMEZONE_INDIA));
        placesTimezoneDB.put("Manali, India", new PlacesInfo(77.1892, 32.2432, TIMEZONE_INDIA));
        placesTimezoneDB.put("Shimla, India", new PlacesInfo(77.1734, 31.1048, TIMEZONE_INDIA));
        placesTimezoneDB.put("Dehradun, India", new PlacesInfo(78.0322, 30.3165, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kedarnath, India", new PlacesInfo(79.0669, 30.7346, TIMEZONE_INDIA));
        placesTimezoneDB.put("Badrinath, India", new PlacesInfo(79.4938, 30.7433, TIMEZONE_INDIA));
        placesTimezoneDB.put("Lucknow, India", new PlacesInfo(80.9462, 26.8467, TIMEZONE_INDIA));
        placesTimezoneDB.put("Prayagraj, India", new PlacesInfo(81.8463, 25.4358, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ayodhya, India", new PlacesInfo(82.1998, 26.7922, TIMEZONE_INDIA));
        placesTimezoneDB.put("Haridwar, India", new PlacesInfo(78.1642, 29.9457, TIMEZONE_INDIA));
        placesTimezoneDB.put("Rishikesh, India", new PlacesInfo(78.2676, 30.0869, TIMEZONE_INDIA));
        placesTimezoneDB.put("Varanasi, India", new PlacesInfo(82.9739, 25.3176, TIMEZONE_INDIA));
        placesTimezoneDB.put("Gaya, India", new PlacesInfo(85.0002, 24.7914, TIMEZONE_INDIA));
        placesTimezoneDB.put("Patna, India", new PlacesInfo(85.1376, 25.5941, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ranchi, India", new PlacesInfo(85.3096, 23.3441, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bodhgaya, India", new PlacesInfo(84.9870, 24.6961, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kanpur, India", new PlacesInfo(80.3319, 26.4499, TIMEZONE_INDIA));
        placesTimezoneDB.put("Jammu, India", new PlacesInfo(74.8570, 32.7266, TIMEZONE_INDIA));
        placesTimezoneDB.put("Srinagar, India", new PlacesInfo(74.7973, 34.0837, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ladakh, India", new PlacesInfo(77.5619, 34.2268, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kargil, India", new PlacesInfo(76.1349, 34.5539, TIMEZONE_INDIA));
        placesTimezoneDB.put("Nagpur, India", new PlacesInfo(79.0882, 21.1458, TIMEZONE_INDIA));
        placesTimezoneDB.put("Shirdi, India", new PlacesInfo(74.4762, 19.7645, TIMEZONE_INDIA));
        placesTimezoneDB.put("Nashik, India", new PlacesInfo(73.7898, 19.9975, TIMEZONE_INDIA));
        placesTimezoneDB.put("Pune, India", new PlacesInfo(73.8567, 18.5204, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kolhapur, India", new PlacesInfo(74.2433, 16.7050, TIMEZONE_INDIA));
        placesTimezoneDB.put("Latur, India", new PlacesInfo(76.5604, 18.4088, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thiruvananthapuram, India", new PlacesInfo(76.9366, 8.5241, TIMEZONE_INDIA));
        placesTimezoneDB.put("Palaghat, India", new PlacesInfo(76.6548, 10.7867, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bengaluru, India", new PlacesInfo(77.5946, 12.9716, TIMEZONE_INDIA));
        placesTimezoneDB.put("Mangaluru, India", new PlacesInfo(74.8560, 12.9141, TIMEZONE_INDIA));
        placesTimezoneDB.put("Belagavi, India", new PlacesInfo(74.4977, 15.8497, TIMEZONE_INDIA));
        placesTimezoneDB.put("Udipi, India", new PlacesInfo(74.7421, 13.3409, TIMEZONE_INDIA));
        placesTimezoneDB.put("Sringeri, India", new PlacesInfo(75.2567, 13.4198, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bagalkot, India", new PlacesInfo(75.6615, 16.1691, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kalaburgi, India", new PlacesInfo(76.8343, 17.3297, TIMEZONE_INDIA));
        placesTimezoneDB.put("Hyderabad, India", new PlacesInfo(78.4867, 17.3850, TIMEZONE_INDIA));
        placesTimezoneDB.put("Srisailam, India", new PlacesInfo(78.8687, 16.0733, TIMEZONE_INDIA));
        placesTimezoneDB.put("Vishakapatnam, India", new PlacesInfo(83.2185, 17.6868, TIMEZONE_INDIA));
        placesTimezoneDB.put("Surat, India", new PlacesInfo(72.8311, 21.1702, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thane, India", new PlacesInfo(72.9781, 19.2183, TIMEZONE_INDIA));
        placesTimezoneDB.put("Indore, India", new PlacesInfo(75.8577, 22.7196, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bhopal, India", new PlacesInfo(77.4126, 23.2599, TIMEZONE_INDIA));
        placesTimezoneDB.put("Vadodara, India", new PlacesInfo(73.1812, 22.3072, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ghaziabad, India", new PlacesInfo(77.4538, 28.6692, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ludhiana, India", new PlacesInfo(75.8573, 30.9010, TIMEZONE_INDIA));
        placesTimezoneDB.put("Agra, India", new PlacesInfo(78.0081, 27.1767, TIMEZONE_INDIA));
        placesTimezoneDB.put("Meerut, India", new PlacesInfo(77.7064, 28.9845, TIMEZONE_INDIA));
        placesTimezoneDB.put("Rajkot, India", new PlacesInfo(70.8022, 22.3039, TIMEZONE_INDIA));
        placesTimezoneDB.put("Sambhaji Nagar, India", new PlacesInfo(75.3433, 19.8762, TIMEZONE_INDIA));
        placesTimezoneDB.put("Karnavati, India", new PlacesInfo(72.5714, 23.0225, TIMEZONE_INDIA));
        placesTimezoneDB.put("Jaipur, India", new PlacesInfo(75.7873, 26.9124, TIMEZONE_INDIA));
        placesTimezoneDB.put("Dhanbad, India", new PlacesInfo(86.4304, 23.7957, TIMEZONE_INDIA));
        placesTimezoneDB.put("Amritsar, India", new PlacesInfo(74.8723, 31.6340, TIMEZONE_INDIA));
        placesTimezoneDB.put("Navi Mumbai, India", new PlacesInfo(73.0297, 19.0330, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kalyan-Dombivli, India", new PlacesInfo(73.1305, 19.2403, TIMEZONE_INDIA));
        placesTimezoneDB.put("Faridabad, India", new PlacesInfo(77.3178, 28.4089, TIMEZONE_INDIA));
        placesTimezoneDB.put("Vasai-Virar, India", new PlacesInfo(72.8397, 19.3919, TIMEZONE_INDIA));
        placesTimezoneDB.put("Howrah, India", new PlacesInfo(88.2636, 22.5958, TIMEZONE_INDIA));
        placesTimezoneDB.put("Jabalpur, India", new PlacesInfo(79.9864, 23.1815, TIMEZONE_INDIA));
        placesTimezoneDB.put("Madurai, India", new PlacesInfo(78.1198, 9.9252, TIMEZONE_INDIA));
        placesTimezoneDB.put("Vijayawada, India", new PlacesInfo(80.6480, 16.5062, TIMEZONE_INDIA));
        placesTimezoneDB.put("Jodhpur, India", new PlacesInfo(73.0243, 26.2389, TIMEZONE_INDIA));
        placesTimezoneDB.put("Raipur, India", new PlacesInfo(81.6296, 21.2514, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kota, India", new PlacesInfo(75.8648, 25.2138, TIMEZONE_INDIA));
        placesTimezoneDB.put("Chandigarh, India", new PlacesInfo(76.7794, 30.7333, TIMEZONE_INDIA));
        placesTimezoneDB.put("Guwahati, India", new PlacesInfo(91.7362, 26.1445, TIMEZONE_INDIA));
        placesTimezoneDB.put("Solapur, India", new PlacesInfo(75.9064, 17.6599, TIMEZONE_INDIA));
        placesTimezoneDB.put("Hubballi, India", new PlacesInfo(75.1240, 15.3647, TIMEZONE_INDIA));
        placesTimezoneDB.put("Mysuru, India", new PlacesInfo(76.6394, 12.2958, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thiruchirappalli, India", new PlacesInfo(78.7047, 10.7905, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bareilly, India", new PlacesInfo(79.4304, 28.3670, TIMEZONE_INDIA));
        placesTimezoneDB.put("Aligarh, India", new PlacesInfo(78.0880, 27.8974, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thiruppur, India", new PlacesInfo(77.3411, 11.1085, TIMEZONE_INDIA));
        placesTimezoneDB.put("Gurugram, India", new PlacesInfo(77.0266, 28.4595, TIMEZONE_INDIA));
        placesTimezoneDB.put("Moradabad, India", new PlacesInfo(78.7733, 28.8386, TIMEZONE_INDIA));
        placesTimezoneDB.put("Jalandhar, India", new PlacesInfo(75.5762, 31.3260, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bhubaneswar, India", new PlacesInfo(85.8245, 20.2961, TIMEZONE_INDIA));
        placesTimezoneDB.put("Warangal, India", new PlacesInfo(79.5941, 17.9689, TIMEZONE_INDIA));
        placesTimezoneDB.put("Mira-Bhayandar, India", new PlacesInfo(72.8544, 19.2952, TIMEZONE_INDIA));
        placesTimezoneDB.put("Jalgaon, India", new PlacesInfo(75.5626, 21.0077, TIMEZONE_INDIA));
        placesTimezoneDB.put("Guntur, India", new PlacesInfo(80.4365, 16.3067, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bhiwandi, India", new PlacesInfo(73.0483, 19.2813, TIMEZONE_INDIA));
        placesTimezoneDB.put("Saharanpur, India", new PlacesInfo(77.5552, 29.9680, TIMEZONE_INDIA));
        placesTimezoneDB.put("Gorakhpur, India", new PlacesInfo(83.3732, 26.7606, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bikaner, India", new PlacesInfo(73.3119, 28.0229, TIMEZONE_INDIA));
        placesTimezoneDB.put("Amravati, India", new PlacesInfo(77.7523, 20.9320, TIMEZONE_INDIA));
        placesTimezoneDB.put("Noida, India", new PlacesInfo(77.3910, 28.5355, TIMEZONE_INDIA));
        placesTimezoneDB.put("Jamshedpur, India", new PlacesInfo(86.2029, 22.8046, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bhilai, India", new PlacesInfo(81.3509, 21.1938, TIMEZONE_INDIA));
        placesTimezoneDB.put("Cuttack, India", new PlacesInfo(85.8830, 20.4625, TIMEZONE_INDIA));
        placesTimezoneDB.put("Firozabad, India", new PlacesInfo(78.3957, 27.1592, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kochi, India", new PlacesInfo(76.2673, 9.9312, TIMEZONE_INDIA));
        placesTimezoneDB.put("Nellore, India", new PlacesInfo(79.9865, 14.4426, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bhavnagar, India", new PlacesInfo(72.1519, 21.7645, TIMEZONE_INDIA));
        placesTimezoneDB.put("Durgapur, India", new PlacesInfo(87.3119, 23.5204, TIMEZONE_INDIA));
        placesTimezoneDB.put("Asansol, India", new PlacesInfo(86.9661, 23.6889, TIMEZONE_INDIA));
        placesTimezoneDB.put("Rourkela, India", new PlacesInfo(84.8536, 22.2604, TIMEZONE_INDIA));
        placesTimezoneDB.put("Nanded, India", new PlacesInfo(77.3210, 19.1383, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ajmer, India", new PlacesInfo(74.6399, 26.4499, TIMEZONE_INDIA));
        placesTimezoneDB.put("Akola, India", new PlacesInfo(77.0082, 20.7002, TIMEZONE_INDIA));
        placesTimezoneDB.put("Jamnagar, India", new PlacesInfo(70.0577, 22.4707, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ujjain, India", new PlacesInfo(75.7885, 23.1765, TIMEZONE_INDIA));
        placesTimezoneDB.put("Loni, India", new PlacesInfo(77.2986, 28.7334, TIMEZONE_INDIA));
        placesTimezoneDB.put("Siliguri, India", new PlacesInfo(88.3953, 26.7271, TIMEZONE_INDIA));
        placesTimezoneDB.put("Jhansi, India", new PlacesInfo(78.5685, 25.4484, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ulhasnagar, India", new PlacesInfo(73.1645, 19.2215, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ambattur, India", new PlacesInfo(80.1548, 13.1143, TIMEZONE_INDIA));
        placesTimezoneDB.put("Rajahmundry, India", new PlacesInfo(81.8040, 17.0005, TIMEZONE_INDIA));
        placesTimezoneDB.put("Malegaon, India", new PlacesInfo(74.5089, 20.5579, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kurnool, India", new PlacesInfo(78.0373, 15.8281, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thirupati, India", new PlacesInfo(79.4192, 13.6288, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thirumala, India", new PlacesInfo(79.3509, 13.6288, TIMEZONE_INDIA));
        placesTimezoneDB.put("Udaipur, India", new PlacesInfo(73.7125, 24.5854, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kakinada, India", new PlacesInfo(82.2475, 16.9891, TIMEZONE_INDIA));
        placesTimezoneDB.put("Davanagere, India", new PlacesInfo(75.9218, 14.4644, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kozhikode, India", new PlacesInfo(75.7804, 11.2588, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bokaro, India", new PlacesInfo(86.1511, 23.6693, TIMEZONE_INDIA));
        placesTimezoneDB.put("South Dumdum, India", new PlacesInfo(88.3983, 22.6089, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ballari, India", new PlacesInfo(76.9214, 15.1394, TIMEZONE_INDIA));
        placesTimezoneDB.put("Patiala, India", new PlacesInfo(76.3869, 30.3398, TIMEZONE_INDIA));
        placesTimezoneDB.put("Gopalpur, India", new PlacesInfo(84.8620, 19.2647, TIMEZONE_INDIA));
        placesTimezoneDB.put("Agartala, India", new PlacesInfo(91.2868, 23.8315, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bhagalpur, India", new PlacesInfo(86.9842, 25.2425, TIMEZONE_INDIA));
        placesTimezoneDB.put("Laxmi Nagar, India", new PlacesInfo(77.7085, 29.4727, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bhatpara, India", new PlacesInfo(88.3912, 22.8536, TIMEZONE_INDIA));
        placesTimezoneDB.put("Panihati, India", new PlacesInfo(88.4037, 22.6939, TIMEZONE_INDIA));
        placesTimezoneDB.put("Dhule, India", new PlacesInfo(74.7749, 20.9042, TIMEZONE_INDIA));
        placesTimezoneDB.put("Rohtak, India", new PlacesInfo(76.6066, 28.8955, TIMEZONE_INDIA));
        placesTimezoneDB.put("Sagar, India", new PlacesInfo(78.7378, 23.8388, TIMEZONE_INDIA));
        placesTimezoneDB.put("Korba, India", new PlacesInfo(82.7501, 22.3595, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bhilwara, India", new PlacesInfo(74.6313, 25.3407, TIMEZONE_INDIA));
        placesTimezoneDB.put("Brahmapur, India", new PlacesInfo(84.7941, 19.3150, TIMEZONE_INDIA));
        placesTimezoneDB.put("Muzaffarpur, India", new PlacesInfo(85.3910, 26.1197, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ahmednagar, India", new PlacesInfo(74.7480, 19.0948, TIMEZONE_INDIA));
        placesTimezoneDB.put("Mathura, India", new PlacesInfo(77.6737, 27.4924, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kollam, India", new PlacesInfo(76.6141, 8.8932, TIMEZONE_INDIA));
        placesTimezoneDB.put("Avadi, India", new PlacesInfo(80.0970, 13.1067, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kadapa, India", new PlacesInfo(78.8242, 14.4673, TIMEZONE_INDIA));
        placesTimezoneDB.put("Anantapuram, India", new PlacesInfo(77.6006, 14.6819, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kamarhati, India", new PlacesInfo(88.3706, 22.6847, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bilaspur, India", new PlacesInfo(82.1409, 22.0797, TIMEZONE_INDIA));
        placesTimezoneDB.put("Sambalpur, India", new PlacesInfo(83.9812, 21.4669, TIMEZONE_INDIA));
        placesTimezoneDB.put("Satara, India", new PlacesInfo(74.0183, 17.6805, TIMEZONE_INDIA));
        placesTimezoneDB.put("Vijayapura, India", new PlacesInfo(75.7100, 16.8302, TIMEZONE_INDIA));
        placesTimezoneDB.put("Rampur, India", new PlacesInfo(79.0220, 28.7983, TIMEZONE_INDIA));
        placesTimezoneDB.put("Shivamogga, India", new PlacesInfo(75.5681, 13.9299, TIMEZONE_INDIA));
        placesTimezoneDB.put("Chandrapur, India", new PlacesInfo(79.2961, 19.9615, TIMEZONE_INDIA));
        placesTimezoneDB.put("Junagadh, India", new PlacesInfo(70.4579, 21.5222, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thrissur, India", new PlacesInfo(76.2144, 10.5276, TIMEZONE_INDIA));
        placesTimezoneDB.put("Alwar, India", new PlacesInfo(76.6346, 27.5530, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bardhaman, India", new PlacesInfo(87.8615, 23.2324, TIMEZONE_INDIA));
        placesTimezoneDB.put("Parbhani, India", new PlacesInfo(76.7748, 19.2608, TIMEZONE_INDIA));
        placesTimezoneDB.put("Tumakuru, India", new PlacesInfo(77.1173, 13.3379, TIMEZONE_INDIA));
        placesTimezoneDB.put("Khammam, India", new PlacesInfo(80.1514, 17.2473, TIMEZONE_INDIA));
        placesTimezoneDB.put("Uzhavarkarai, India", new PlacesInfo(79.7733, 11.9394, TIMEZONE_INDIA));
        placesTimezoneDB.put("Panipat, India", new PlacesInfo(76.9635, 29.3909, TIMEZONE_INDIA));
        placesTimezoneDB.put("Darbhanga, India", new PlacesInfo(85.8918, 26.1542, TIMEZONE_INDIA));
        placesTimezoneDB.put("Aizawl, India", new PlacesInfo(92.7173, 23.7307, TIMEZONE_INDIA));
        placesTimezoneDB.put("Dewas, India", new PlacesInfo(76.0534, 22.9676, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ichalkaranji, India", new PlacesInfo(74.4593, 16.6886, TIMEZONE_INDIA));
        placesTimezoneDB.put("Karnal, India", new PlacesInfo(76.9905, 29.6857, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bathinda, India", new PlacesInfo(74.9455, 30.2110, TIMEZONE_INDIA));
        placesTimezoneDB.put("Jalna, India", new PlacesInfo(75.8816, 19.8347, TIMEZONE_INDIA));
        placesTimezoneDB.put("Eluru, India", new PlacesInfo(81.0952, 16.7107, TIMEZONE_INDIA));
        placesTimezoneDB.put("Barasat, India", new PlacesInfo(88.4789, 22.7248, TIMEZONE_INDIA));
        placesTimezoneDB.put("Purnia, India", new PlacesInfo(87.4753, 25.7771, TIMEZONE_INDIA));
        placesTimezoneDB.put("Puri, India", new PlacesInfo(85.8312, 19.8135, TIMEZONE_INDIA));
        placesTimezoneDB.put("Satna, India", new PlacesInfo(80.8322, 24.6005, TIMEZONE_INDIA));
        placesTimezoneDB.put("Sonipat, India", new PlacesInfo(77.0151, 28.9931, TIMEZONE_INDIA));
        placesTimezoneDB.put("Imphal, India", new PlacesInfo(93.9368, 24.8170, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ratlam, India", new PlacesInfo(75.0367, 23.3315, TIMEZONE_INDIA));
        placesTimezoneDB.put("Anantapur, India", new PlacesInfo(77.6006, 14.6819, TIMEZONE_INDIA));
        placesTimezoneDB.put("Karimnagar, India", new PlacesInfo(79.1288, 18.4386, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ambarnath, India", new PlacesInfo(73.1926, 19.1825, TIMEZONE_INDIA));
        placesTimezoneDB.put("North Dumdum, India", new PlacesInfo(88.4090, 22.6626, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bharatpur, India", new PlacesInfo(77.5030, 27.2152, TIMEZONE_INDIA));
        placesTimezoneDB.put("Begusarai, India", new PlacesInfo(86.1272, 25.4182, TIMEZONE_INDIA));
        placesTimezoneDB.put("Gandhidham, India", new PlacesInfo(70.1337, 23.0753, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thiruvottiyur, India", new PlacesInfo(80.3001, 13.1643, TIMEZONE_INDIA));
        placesTimezoneDB.put("Pondicherry, India", new PlacesInfo(79.8083, 11.9416, TIMEZONE_INDIA));
        placesTimezoneDB.put("Sikar, India", new PlacesInfo(75.1398, 27.6094, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thoothukudi, India", new PlacesInfo(78.1348, 8.7642, TIMEZONE_INDIA));
        placesTimezoneDB.put("Rewa, India", new PlacesInfo(81.3037, 24.5362, TIMEZONE_INDIA));
        placesTimezoneDB.put("Pali, India", new PlacesInfo(73.3311, 25.7781, TIMEZONE_INDIA));
        placesTimezoneDB.put("Raichur, India", new PlacesInfo(77.3566, 16.2160, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ramagundam, India", new PlacesInfo(79.5134, 18.7519, TIMEZONE_INDIA));
        placesTimezoneDB.put("Rameswaram, India", new PlacesInfo(79.3129, 9.2876, TIMEZONE_INDIA));
        placesTimezoneDB.put("Silchar, India", new PlacesInfo(92.7789, 24.8333, TIMEZONE_INDIA));
        placesTimezoneDB.put("Vijayanagaram, India", new PlacesInfo(83.3956, 18.1067, TIMEZONE_INDIA));
        placesTimezoneDB.put("Tenali, India", new PlacesInfo(80.6444, 16.2379, TIMEZONE_INDIA));
        placesTimezoneDB.put("Nagercoil, India", new PlacesInfo(77.4119, 8.1833, TIMEZONE_INDIA));
        placesTimezoneDB.put("Sri Ganganagar, India", new PlacesInfo(73.8800, 29.9094, TIMEZONE_INDIA));
        placesTimezoneDB.put("Sambhal, India", new PlacesInfo(78.5718, 28.5904, TIMEZONE_INDIA));
        placesTimezoneDB.put("Mango, India", new PlacesInfo(86.2294, 22.8384, TIMEZONE_INDIA));
        placesTimezoneDB.put("Nadiad, India", new PlacesInfo(72.8634, 22.6916, TIMEZONE_INDIA));
        placesTimezoneDB.put("Secunderabad, India", new PlacesInfo(78.4983, 17.4399, TIMEZONE_INDIA));
        placesTimezoneDB.put("Naihati, India", new PlacesInfo(88.4220, 22.8895, TIMEZONE_INDIA));
        placesTimezoneDB.put("Yamunanagar, India", new PlacesInfo(77.2674, 30.1290, TIMEZONE_INDIA));
        placesTimezoneDB.put("Pallavaram, India", new PlacesInfo(80.1491, 12.9675, TIMEZONE_INDIA));
        placesTimezoneDB.put("Dindigul, India", new PlacesInfo(77.9695, 10.3624, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kharagpur, India", new PlacesInfo(87.2320, 22.3460, TIMEZONE_INDIA));
        placesTimezoneDB.put("Hosapete, India", new PlacesInfo(76.3909, 15.2689, TIMEZONE_INDIA));
        placesTimezoneDB.put("Gandhinagar, India", new PlacesInfo(72.6369, 23.2156, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ongole, India", new PlacesInfo(80.0499, 15.4777, TIMEZONE_INDIA));
        placesTimezoneDB.put("Nandyal, India", new PlacesInfo(78.4873, 13.0827, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bhiwani, India", new PlacesInfo(76.1322, 28.7975, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ambala, India", new PlacesInfo(76.7821, 30.3752, TIMEZONE_INDIA));
        placesTimezoneDB.put("Chittoor, India", new PlacesInfo(79.1003, 13.2172, TIMEZONE_INDIA));
        placesTimezoneDB.put("Vellore, India", new PlacesInfo(79.1325, 12.9165, TIMEZONE_INDIA));
        placesTimezoneDB.put("Alappuzha, India", new PlacesInfo(76.3388, 9.4981, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kottayam, India", new PlacesInfo(76.5222, 9.5916, TIMEZONE_INDIA));
        placesTimezoneDB.put("Machilipatnam, India", new PlacesInfo(81.1303, 16.1809, TIMEZONE_INDIA));
        placesTimezoneDB.put("Hindupur, India", new PlacesInfo(77.5009, 13.8223, TIMEZONE_INDIA));
        placesTimezoneDB.put("Udupi, India", new PlacesInfo(74.7421, 13.3409, TIMEZONE_INDIA));
        placesTimezoneDB.put("Port Blair, India", new PlacesInfo(92.7265, 11.6234, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bhimavaram, India", new PlacesInfo(81.5212, 16.5449, TIMEZONE_INDIA));
        placesTimezoneDB.put("Madanapalle, India", new PlacesInfo(78.5010, 13.5560, TIMEZONE_INDIA));
        placesTimezoneDB.put("Guntakal, India", new PlacesInfo(77.3770, 15.1661, TIMEZONE_INDIA));
        placesTimezoneDB.put("Dharmavaram, India", new PlacesInfo(77.7201, 14.4125, TIMEZONE_INDIA));
        placesTimezoneDB.put("Srikakulam, India", new PlacesInfo(83.8938, 18.2949, TIMEZONE_INDIA));
        placesTimezoneDB.put("Pudukkottai, India", new PlacesInfo(78.8001, 10.3833, TIMEZONE_INDIA));
        placesTimezoneDB.put("Hosur, India", new PlacesInfo(77.8253, 12.7409, TIMEZONE_INDIA));
        placesTimezoneDB.put("Tadipatri, India", new PlacesInfo(78.0092, 14.9091, TIMEZONE_INDIA));
        placesTimezoneDB.put("Karaikudi, India", new PlacesInfo(78.7803, 10.0763, TIMEZONE_INDIA));
        placesTimezoneDB.put("Gangtok, India", new PlacesInfo(88.6138, 27.3314, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kavaratti, India", new PlacesInfo(72.6358, 10.5593, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kanyakumari, India", new PlacesInfo(77.5385, 8.0883, TIMEZONE_INDIA));
        placesTimezoneDB.put("Nagapattinam, India", new PlacesInfo(79.8449, 10.7672, TIMEZONE_INDIA));
        placesTimezoneDB.put("Pollachi, India", new PlacesInfo(77.0048, 10.6609, TIMEZONE_INDIA));
        placesTimezoneDB.put("Chettinad, India", new PlacesInfo(78.7773, 10.1606, TIMEZONE_INDIA));
        placesTimezoneDB.put("Courtallam, India", new PlacesInfo(77.2779, 8.9341, TIMEZONE_INDIA));
        placesTimezoneDB.put("Mahabalipuram, India", new PlacesInfo(80.1945, 12.6208, TIMEZONE_INDIA));
        placesTimezoneDB.put("Ooty, India", new PlacesInfo(76.6950, 11.4102, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kodaikanal, India", new PlacesInfo(77.4892, 10.2381, TIMEZONE_INDIA));
        placesTimezoneDB.put("Coonoor, India", new PlacesInfo(76.7959, 11.3530, TIMEZONE_INDIA));
        placesTimezoneDB.put("Yelagiri, India", new PlacesInfo(78.6345, 12.5856, TIMEZONE_INDIA));
        placesTimezoneDB.put("Yercaud, India", new PlacesInfo(78.2097, 11.7748, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kotagiri, India", new PlacesInfo(76.8617, 11.4218, TIMEZONE_INDIA));
        placesTimezoneDB.put("Valparai, India", new PlacesInfo(76.9554, 10.3270, TIMEZONE_INDIA));
        placesTimezoneDB.put("Theni, India", new PlacesInfo(77.4768, 10.0104, TIMEZONE_INDIA));
        placesTimezoneDB.put("Vedanthangal, India", new PlacesInfo(79.8561, 12.5455, TIMEZONE_INDIA));
        placesTimezoneDB.put("Palani, India", new PlacesInfo(77.5161, 10.4500, TIMEZONE_INDIA));
        placesTimezoneDB.put("Mayiladuthurai, India", new PlacesInfo(79.6526, 11.1018, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thiruvarur, India", new PlacesInfo(79.6344, 10.7661, TIMEZONE_INDIA));
        placesTimezoneDB.put("Bodinayakkanur, India", new PlacesInfo(77.3497, 10.0106, TIMEZONE_INDIA));
        placesTimezoneDB.put("Mudumalai, India", new PlacesInfo(76.5257, 11.6376, TIMEZONE_INDIA));
        placesTimezoneDB.put("Dharmapuri, India", new PlacesInfo(78.1582, 12.1211, TIMEZONE_INDIA));
        placesTimezoneDB.put("Nilgiris, India", new PlacesInfo(76.7337, 11.4916, TIMEZONE_INDIA));
        placesTimezoneDB.put("Dhanushkodi, India", new PlacesInfo(79.4183, 9.1794, TIMEZONE_INDIA));
        placesTimezoneDB.put("Krishnagiri, India", new PlacesInfo(78.2150, 12.5266, TIMEZONE_INDIA));
        placesTimezoneDB.put("Auroville, India", new PlacesInfo(79.8069, 12.0052, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kolli Hills, India", new PlacesInfo(78.3387, 11.2485, TIMEZONE_INDIA));
        placesTimezoneDB.put("Thiruttani, India", new PlacesInfo(79.6117, 13.1746, TIMEZONE_INDIA));
        placesTimezoneDB.put("Namakkal, India", new PlacesInfo(78.1674, 11.2189, TIMEZONE_INDIA));
        placesTimezoneDB.put("Papanasam, India", new PlacesInfo(79.2864, 10.9233, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kalancheri, India", new PlacesInfo(79.2687, 10.8144, TIMEZONE_INDIA));
        placesTimezoneDB.put("Kalladaikurichi, India", new PlacesInfo(77.4651, 8.6830, TIMEZONE_INDIA));
        placesTimezoneDB.put("Gudur, India", new PlacesInfo(79.8504, 14.1463, TIMEZONE_INDIA));
        placesTimezoneDB.put("Accra, Ghana", new PlacesInfo(-0.1870, 5.6037, "Africa/Accra"));

        // DST Adjusted
        placesTimezoneDB.put("Lisbon, Portugal", new PlacesInfo(-9.1393, 38.7223, "Europe/Lisbon"));
        placesTimezoneDB.put("Dublin, Ireland", new PlacesInfo(-6.2603, 53.3498, "Europe/Dublin"));
        placesTimezoneDB.put("London, UK", new PlacesInfo(-0.1278, 51.5074, "Europe/London"));
        placesTimezoneDB.put("Oslo, Norway", new PlacesInfo(10.7522, 59.9139, "Europe/Oslo"));
        placesTimezoneDB.put("Stockholm, Sweden", new PlacesInfo(18.0686, 59.3293, "Europe/Stockholm"));
        placesTimezoneDB.put("Helsinki, Finland", new PlacesInfo(24.9384, 60.1699, "Europe/Helsinki"));
        placesTimezoneDB.put("Madrid, Spain", new PlacesInfo(-3.7038, 40.4168, "Europe/Madrid"));
        placesTimezoneDB.put("Paris, France", new PlacesInfo(2.3522, 48.8566, "Europe/Paris"));
        placesTimezoneDB.put("Frankfurt, Germany", new PlacesInfo(8.6821, 50.1109, "Europe/Berlin"));
        placesTimezoneDB.put("Munich, Germany", new PlacesInfo(11.5820, 48.1351, "Europe/Berlin"));
        placesTimezoneDB.put("Vienna, Austria", new PlacesInfo(16.3738, 48.2082, "Europe/Vienna"));
        placesTimezoneDB.put("Rome, Italy", new PlacesInfo(12.4964, 41.9028, "Europe/Rome"));
        placesTimezoneDB.put("Venice, Italy", new PlacesInfo(12.3155, 45.4408, "Europe/Rome"));
        placesTimezoneDB.put("Zurich, Switzerland", new PlacesInfo(8.5417, 47.3769, "Europe/Zurich"));
        placesTimezoneDB.put("Bern, Switzerland", new PlacesInfo(7.4474, 46.9480, "Europe/Zurich"));
        placesTimezoneDB.put("Moosseedorf, Switzerland", new PlacesInfo(7.4846, 47.0146, "Europe/Zurich"));
        placesTimezoneDB.put("Brussels, Belgium", new PlacesInfo(4.3517, 50.8503, "Europe/Brussels"));
        placesTimezoneDB.put("Warsaw, Poland", new PlacesInfo(21.0122, 52.2297, "Europe/Warsaw"));
        placesTimezoneDB.put("Prague, Czech Republic", new PlacesInfo(14.4378, 50.0755, "Europe/Prague"));
        placesTimezoneDB.put("Budapest, Hungary", new PlacesInfo(19.0402, 47.4979, "Europe/Budapest"));
        placesTimezoneDB.put("Bucharest, Romania", new PlacesInfo(26.1025, 44.4268, "Europe/Bucharest"));
        placesTimezoneDB.put("Athens, Greece", new PlacesInfo(23.7275, 37.9838, "Europe/Athens"));
        placesTimezoneDB.put("Jerusalem, Israel", new PlacesInfo(35.2137, 31.7683, "Asia/Jerusalem"));
        placesTimezoneDB.put("Tehran, Iran", new PlacesInfo(51.3890, 35.6892, "Asia/Tehran"));

        // No DST Adjustment needed!
        placesTimezoneDB.put("Cairo, Egypt", new PlacesInfo(31.2357, 30.0444, "Africa/Cairo"));
        placesTimezoneDB.put("Johannesburg, South Africa", new PlacesInfo(28.0473, -26.2041, "Africa/Johannesburg"));
        placesTimezoneDB.put("Durban, South Africa", new PlacesInfo(31.0218, -29.8587, "Africa/Johannesburg"));
        placesTimezoneDB.put("Lusaka, Zambia", new PlacesInfo(28.3228, -15.3875, "Africa/Lusaka"));
        placesTimezoneDB.put("Harare, Zimbabwe", new PlacesInfo(31.0492, -17.8216, "Africa/Harare"));
        placesTimezoneDB.put("Mogadishu, Somalia", new PlacesInfo(45.3182, 2.0469, "Africa/Mogadishu"));
        placesTimezoneDB.put("Nairobi, Kenya", new PlacesInfo(36.8219, -1.2921, "Africa/Nairobi"));
        placesTimezoneDB.put("Moscow, Russia", new PlacesInfo(37.6173, 55.7558, "Europe/Moscow"));
        placesTimezoneDB.put("Ankara, Turkey", new PlacesInfo(32.8597, 39.9334, "Europe/Moscow"));
        placesTimezoneDB.put("Baghdad, Iraq", new PlacesInfo(44.3661, 33.3152, "Asia/Baghdad"));
        placesTimezoneDB.put("Riyadh, Saudi Arabia", new PlacesInfo(46.6753, 24.7136, "Asia/Riyadh"));
        placesTimezoneDB.put("Doha, Qatar", new PlacesInfo(51.5310, 25.2854, "Asia/Qatar"));
        placesTimezoneDB.put("Kuwait, Kuwait", new PlacesInfo(47.4818, 29.3117, "Asia/Kuwait"));
        placesTimezoneDB.put("Dubai, United Arab Emirates", new PlacesInfo(55.2708, 25.2048, "Asia/Dubai"));
        placesTimezoneDB.put("Abu Dhabi, United Arab Emirates", new PlacesInfo(54.3773, 24.4539, "Asia/Dubai"));
        placesTimezoneDB.put("Muscat, Oman", new PlacesInfo(58.3829, 23.5880, "Asia/Muscat"));
        placesTimezoneDB.put("Kabul, Afganistan", new PlacesInfo(69.2075, 34.5553, "Asia/Kabul"));
        placesTimezoneDB.put("Colombo, Srilanka", new PlacesInfo(79.8612, 6.9271, "Asia/Colombo"));
        placesTimezoneDB.put("Islamabad, Pakistan", new PlacesInfo(73.0479, 33.6844, "Asia/Karachi"));
        placesTimezoneDB.put("Dhaka, Bangladesh", new PlacesInfo(90.4125, 23.8103, "Asia/Dhaka"));
        placesTimezoneDB.put("Jakarta, Indonesia", new PlacesInfo(106.8456, -6.2088, "Asia/Jakarta"));
        placesTimezoneDB.put("Taipei, Taiwan", new PlacesInfo(121.5654, 25.0330, "Asia/Taipei"));
        placesTimezoneDB.put("Singapore", new PlacesInfo(103.8198, 1.3521, "Asia/Singapore"));
        placesTimezoneDB.put("Hongkong", new PlacesInfo(114.1694, 22.3193, "Asia/Hong_Kong"));
        placesTimezoneDB.put("Beijing, China", new PlacesInfo(116.4074, 39.9042, "Asia/Shanghai"));
        placesTimezoneDB.put("Perth, Australia", new PlacesInfo(115.8613, -31.9523, "Australia/Perth"));
        placesTimezoneDB.put("Tokyo, Japan", new PlacesInfo(139.6503, 35.6762, "Asia/Tokyo"));
        placesTimezoneDB.put("Kawasaki, Japan", new PlacesInfo(139.7029, 35.5308, "Asia/Tokyo"));
        placesTimezoneDB.put("Kyoto, Japan", new PlacesInfo(135.7681, 35.0116, "Asia/Tokyo"));
        placesTimezoneDB.put("Matsumoto, Japan", new PlacesInfo(137.9720, 36.2380, "Asia/Tokyo"));
        placesTimezoneDB.put("Seoul, South Korea", new PlacesInfo(126.9780, 37.5665, "Asia/Seoul"));

        // DST Adjusted
        placesTimezoneDB.put("Melbourne, Australia", new PlacesInfo(144.9631, -37.8136, "Australia/Melbourne"));
        placesTimezoneDB.put("Adelaide, Australia", new PlacesInfo(138.6007, -34.9285, "Australia/Adelaide"));
        placesTimezoneDB.put("Sydney, Australia", new PlacesInfo(151.2093, -33.8688, "Australia/Sydney"));
        placesTimezoneDB.put("Canberra, Australia", new PlacesInfo(149.1300, -35.2809, "Australia/Canberra"));
        placesTimezoneDB.put("Brisbane, Australia", new PlacesInfo(153.0260, -27.4705, "Australia/Brisbane"));
        placesTimezoneDB.put("Hobart, Australia", new PlacesInfo(147.3257, -42.8826, "Australia/Hobart"));
        placesTimezoneDB.put("Christchurch, New Zealand", new PlacesInfo(172.6306, -43.5320, "Pacific/Auckland"));
        placesTimezoneDB.put("Wellington, New Zealand", new PlacesInfo(174.7787, -41.2924, "Pacific/Auckland"));
        placesTimezoneDB.put("Waikiki, USA", new PlacesInfo(-157.8292, 21.2793, "Pacific/Honolulu"));
        placesTimezoneDB.put("Hawaii, USA", new PlacesInfo(-155.5828, 19.8968, "Pacific/Honolulu"));
        placesTimezoneDB.put("Anchorage, USA", new PlacesInfo(-149.9003, 61.2181, "America/Anchorage"));
        placesTimezoneDB.put("San Francisco, USA", new PlacesInfo(-122.4194, 37.7749, "America/Los_Angeles"));
        placesTimezoneDB.put("San Jose, USA", new PlacesInfo(-121.8863, 37.3382, "America/Los_Angeles"));
        placesTimezoneDB.put("Fremont, USA", new PlacesInfo(-121.9886, 37.5485, "America/Los_Angeles"));
        placesTimezoneDB.put("Las Vegas, USA", new PlacesInfo(-115.1398, 36.1699, "America/Los_Angeles"));
        placesTimezoneDB.put("Seattle, USA", new PlacesInfo(-122.3321, 47.6062, "America/Los_Angeles"));
        placesTimezoneDB.put("Portland, USA", new PlacesInfo(-122.6784, 45.5152, "America/Los_Angeles"));
        placesTimezoneDB.put("Phoenix, USA", new PlacesInfo(-112.0740, 33.4484, "America/Denver"));
        placesTimezoneDB.put("Denver, USA", new PlacesInfo(-104.9903, 39.7392, "America/Denver"));
        placesTimezoneDB.put("Memphis, USA", new PlacesInfo(-90.0490, 35.1495, "America/Chicago"));
        placesTimezoneDB.put("Winnipeg, Canada", new PlacesInfo(-97.1384, 49.8951, "America/Winnipeg"));
        placesTimezoneDB.put("Dallas, USA", new PlacesInfo(-96.7970, 32.7767, "America/Chicago"));
        placesTimezoneDB.put("Kansas, USA", new PlacesInfo(-98.4842, 39.0119, "America/Chicago"));
        placesTimezoneDB.put("Mexico city, Mexico", new PlacesInfo(-99.1332, 19.4326, "America/Mexico_City"));
        placesTimezoneDB.put("Chicago, USA", new PlacesInfo(-87.6298, 41.8781, "America/Chicago"));
        placesTimezoneDB.put("Houston, USA", new PlacesInfo(-95.3698, 29.7604, "America/Chicago"));
        placesTimezoneDB.put("Detroit, USA", new PlacesInfo(-83.0458, 42.3314, "America/Detroit"));
        placesTimezoneDB.put("Atlanta, USA", new PlacesInfo(-84.3880, 33.7490, "America/New_York"));
        placesTimezoneDB.put("Charlotte, USA", new PlacesInfo(-80.8431, 35.2271, "America/New_York"));
        placesTimezoneDB.put("Ann Arbor, USA", new PlacesInfo(-83.7430, 42.2808, "America/Detroit"));
        placesTimezoneDB.put("Toronto, Canada", new PlacesInfo(-79.3832, 43.6532, "America/Toronto"));
        placesTimezoneDB.put("St Augustine, USA", new PlacesInfo(-81.3124, 29.9012, "America/New_York"));
        placesTimezoneDB.put("Chester Springs, USA", new PlacesInfo(-75.6343, 40.0784, "America/New_York"));
        placesTimezoneDB.put("Richmond, USA", new PlacesInfo(-77.4360, 37.5407, "America/New_York"));
        placesTimezoneDB.put("New York, USA", new PlacesInfo(-74.0060, 40.7128, "America/New_York"));
        placesTimezoneDB.put("Washington, D.C., USA", new PlacesInfo(-77.0369, 38.9072, "America/New_York"));
        placesTimezoneDB.put("Boston, USA", new PlacesInfo(-71.0589, 42.3601, "America/New_York"));
        placesTimezoneDB.put("Miami, USA", new PlacesInfo(-80.1918, 25.7617, "America/New_York"));
        placesTimezoneDB.put("Tampa, USA", new PlacesInfo(-82.5437, 27.7634, "America/New_York"));
        placesTimezoneDB.put("Jacksonville, USA", new PlacesInfo(-81.6557, 30.3322, "America/New_York"));
        placesTimezoneDB.put("Ottawa, Canada", new PlacesInfo(-75.6972, 45.4215, "America/Toronto"));
        placesTimezoneDB.put("Salem, MA, USA", new PlacesInfo(-70.8967, 42.5195, "America/Chicago"));
        placesTimezoneDB.put("Indianapolis, USA", new PlacesInfo(-86.1581, 39.7684, "America/Indiana/Indianapolis"));

        // No DST Adjustment needed!
        placesTimezoneDB.put("Buenos Aires, Argentina", new PlacesInfo(-58.3816, -34.6037, "America/Argentina/Buenos_Aires"));
        placesTimezoneDB.put("Rio de Janeiro, Brazil", new PlacesInfo(-43.1729, -22.9068, "America/Sao_Paulo"));

        // Add the keys {place names} into an array which can be displayed in a drop-down!
        placesList = new ArrayList<>(placesTimezoneDB.keySet());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(NP_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onBackPressed() {
        boolean defaultHandling = true;
        int curFragment = viewPager.getCurrentItem();
        if (curFragment == NPAdapter.NP_TAB_ALARM) {
            Alarm alarmFragment = (Alarm)myAdapter.getItem(curFragment);

            // Handle Back Pressed event only for Alarms and also only when an alarm row is
            // selected.
            // Use default handling for all other scenarios.
            if (alarmFragment.handleBackPressedEvent()) {
                defaultHandling = false;
            }
        }

        if (defaultHandling) {
            super.onBackPressed();
        }
    }

    /**
     * Fragments use this API to update the locale for all the elements.
     *
     * @return Modified language as a string.
     */
    public String updateAppLocale() {
        SharedPreferences localPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (localPreferences != null) {
            String prefLang = localPreferences.getString(SettingsFragment.PREF_NP_LOCALE_KEY, "En");
            try {
                selLocale = getLocaleShortStr(prefLang);
            } catch (Exception e) {
                // Fallback to default language preference
                selLocale = "en";
            }

            Locale locale = new Locale(selLocale);
            Locale.setDefault(locale);
            Resources resources = getResources();
            Configuration config = resources.getConfiguration();
            config.locale = locale;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }

        return selLocale;
    }

    private static String getLocaleShortStr(String localeStr) {
        String localeShortStr;
        switch (localeStr) {
            case "Tamil":
                localeShortStr = "ta";
                break;
            case "Sanskrit":
                localeShortStr = "sa";
                break;
            case "Telugu":
                localeShortStr = "te";
                break;
            case "Malayalam":
                localeShortStr = "ml";
                break;
            case "Kannada":
                localeShortStr = "kn";
                break;
            case "Hindi":
                localeShortStr = "hi";
                break;
            case "IAST":
                localeShortStr = "inc";
                break;
            default:
                localeShortStr = "en";
                break;
        }

        return localeShortStr;
    }

    public static HashMap<Integer, String[]> buildVedicCalendarLocaleList(Context context) {
        if (vedicCalendarLocaleList.isEmpty()) {

            // Step1: Samvatsaram
            String[] arrayList = context.getResources().getStringArray(R.array.samvatsaram_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_SAMVATSARAM,
                    arrayList);

            // Step2: Ayanam
            arrayList = context.getResources().getStringArray(R.array.ayanam_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_AYANAM, arrayList);

            // Step3: Rithu
            arrayList = context.getResources().getStringArray(R.array.rithu_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_RITHU, arrayList);

            // Step4-1: Maasam (Solar Months)
            arrayList = context.getResources().getStringArray(R.array.sauramaanam_maasam_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_SAURAMANA_MAASAM, arrayList);

            // Step4-2: Maasam (Lunar Months)
            arrayList = context.getResources().getStringArray(R.array.chaandramaanam_maasam_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_CHAANDRAMANA_MAASAM, arrayList);

            // Step5: Paksham
            arrayList = context.getResources().getStringArray(R.array.paksham_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_PAKSHAM, arrayList);

            // Step6: Thithi
            arrayList = context.getResources().getStringArray(R.array.tithi_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_TITHI, arrayList);

            // Step7: Sankalpa Thithi
            arrayList = context.getResources().getStringArray(R.array.sankalpa_tithi_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_TITHI, arrayList);

            // Step8: Raasi
            arrayList = context.getResources().getStringArray(R.array.raasi_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_RAASI, arrayList);

            // Step9: Nakshathram
            arrayList = context.getResources().getStringArray(R.array.nakshathram_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_NAKSHATHRAM, arrayList);

            // Step10: Sankalpa Nakshathram
            arrayList = context.getResources().getStringArray(R.array.sankalpa_nakshathram_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_NAKSHATHRAM, arrayList);

            // Step11: Yogam
            arrayList = context.getResources().getStringArray(R.array.yogam_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_YOGAM, arrayList);

            // Step12: Karanam
            arrayList = context.getResources().getStringArray(R.array.karanam_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_KARANAM, arrayList);

            // Step13: Vaasaram
            arrayList = context.getResources().getStringArray(R.array.vaasaram_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_VAASARAM, arrayList);

            // Step14: Dhinam
            arrayList = context.getResources().getStringArray(R.array.dhinam_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_DINAM, arrayList);

            // Step15: Horai
            arrayList = context.getResources().getStringArray(R.array.horai_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_HORAI, arrayList);

            // Step16: Amruthathi Yogam
            arrayList = context.getResources().getStringArray(R.array.amruthathi_yogam_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_AMRUTATHI_YOGAM, arrayList);

            // Step17: Kaala Vibhagah
            arrayList = context.getResources().getStringArray(R.array.kaala_vibhaagaha_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_KAALA_VIBHAAGAH, arrayList);

            // Step18: Shooam (Parihaaram)
            arrayList = context.getResources().getStringArray(R.array.shoolam_parihaaram_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_SHOOLAM_PARIHAARAM, arrayList);

            // Step19: Dina Drishti List
            arrayList = context.getResources().getStringArray(R.array.dina_drishti_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_DINA_DRISHTI, arrayList);
        }

        return vedicCalendarLocaleList;
    }

    public static void sendBroadcastToWidget(Context context) {
        Intent intent = new Intent(context, NithyaPanchangamWidget.class);
        intent.putExtra(NP_UPDATE_WIDGET, "Update");
        context.sendBroadcast(intent);
    }
}