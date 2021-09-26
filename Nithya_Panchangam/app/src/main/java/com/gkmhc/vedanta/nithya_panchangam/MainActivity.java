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
 *   - Panchangam Type      - Only "Drik Ganitham" is supported.
 *                            TODO - Vakhyam to be added in later versions.
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
 *                             TODO - Vakhyam to be added in later versions.
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
    private int prefAyanamsa = VedicCalendar.AYANAMSA_CHITRAPAKSHA;
    private int prefChaandramanamType = VedicCalendar.CHAANDRAMAANAM_TYPE_AMANTA;
    private static String prefSankalpamType = "";
    private static String selLocale = "en";
    private static String curLocationCity = "";
    private static final double INDIAN_STANDARD_TIME = 5.5;
    public static final String NP_UPDATE_WIDGET = "Nithya_Panchangam_Update_Widget";
    public static final String NP_CHANNEL_ID = "Nithya_Panchangam";
    public static final int LOCATION_MANUAL = 0;
    public static final int LOCATION_GPS = 1;

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
    private static HashMap<String, String[]> vedicCalendarLocaleList = new HashMap<>();

    public static class PlacesInfo {
        public final double longitude;
        public final double latitude;
        public final double timezone;

        PlacesInfo(double longitude, double latitude, double timezone) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.timezone = timezone;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        buildPlacesTimezoneDB();

        curLocationCity = readDefLocationSetting(getApplicationContext());
        refreshLocation();

        // Step 1: Get the preferred locale from preferences and update activity.
        prefSankalpamType = readPrefSankalpamType(this);
        updateAppLocale();
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#0000FF'>" +
                getString(R.string.app_name) + "</font>"));

        // Step 2: Sort out the toolbar icon & logos
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher_round);
        getSupportActionBar().setBackgroundDrawable(
                ResourcesCompat.getDrawable(getResources(), R.drawable.default_background, null));
        getSupportActionBar().setTitle(Html.fromHtml("<font color='#0000FF'>" +
                getString(R.string.app_name) + "</font>"));

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
        //new CopyToAssets(".*\\.se1", getApplicationContext()).copy();
        //new CopyToAssets(".*\\.txt", getApplicationContext()).copy();
        //new CopyToAssets(".*?\\.(se1|xml?)", getApplicationContext()).copy();
        //String localpath = getApplicationContext().getFilesDir() + File.separator + "/ephe";

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(alarmMsgReceiver, new IntentFilter(NPBroadcastReceiver.STOP_ALARM));

        //Log.d("MainActivity","initSwissEph()... Time Taken: " +
        //        VedicCalendar.getTimeTaken(startTime, endTime));
    }

    // Copy all the SwissEph assets to the local directory.
    // On Android, this can be done in the below suggested way.
    //
    // Note: When using VedicCalendar class on other platforms then all the
    // SwissEph assets needs to be copied to the local directory as per the local platform
    // on VedicCalendar is being used.
    public static String getLocalPath(Context context) {
        new CopyToAssets(".*?\\.(se1|xml?)", context).copy();
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
                    Log.d("MainActivity","STOP Alarm Received!");

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
        // If Manual option is selected, then provide a way select a location
        // Use geocoder to retrieve {Location, longitude, latitude}
        updateManualLocation(curLocationCity);
        if (LOCATION_MANUAL != readPrefLocationSelection()) {
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
        } else if (itemID == R.id.calendar_np) {
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
        /*
        TODO - For future revision!
        else if (itemID == R.id.raasi_chart) {
            startActivityForResult(new Intent(this, RaasiChart.class),
                    RAASI_CHART_CODE);
        } else if (itemID == R.id.kaala_vibhagam) {
            startActivityForResult(new Intent(this, NithyaKaalam.class),
                    KAALA_VIBHAGAM_CODE);
        }*/

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
            int ayanamsaMode = readPrefAyanamsaSelection(this);
            PlacesInfo placesInfo = getLocationDetails(location);
            vedicCalendarLocaleList.clear();
            vedicCalendarLocaleList = buildVedicCalendarLocaleList(this);
            vedicCalendar = VedicCalendar.getInstance(
                    getLocalPath(this),
                    readPrefPanchangamType(this), currCalendar, placesInfo.longitude,
                    placesInfo.latitude, placesInfo.timezone, ayanamsaMode,
                    readPrefChaandramanaType(this), vedicCalendarLocaleList);
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
            // If Manual location has changed, then refresh the tabs
            String selectedLocation = readDefLocationSetting(getApplicationContext());
            if (!selectedLocation.equalsIgnoreCase(curLocationCity)) {
                curLocationCity = selectedLocation;
                refreshLocation();
                refreshPanchangamDetails();

                // Send broadcast Intent to widget(s) to refresh!
                sendBroadcastToWidget(this);
            }

            // If there is change in location preferences, then refresh location & the fragments.
            int prefToUpdate = readPrefLocationSelection();
            if (prefLocationType != prefToUpdate) {
                prefLocationType = prefToUpdate;
                refreshLocation();
                refreshPanchangamDetails();

                // Send broadcast Intent to widget(s) to refresh!
                sendBroadcastToWidget(this);
            }

            // If there is change in locale preferences, then refresh location & the fragments.
            String defLocale = readPrefLocale();
            if (!defLocale.equals(selLocale)) {
                refreshPanchangamDetails();
                refreshTab(NPAdapter.NP_TAB_ALARM);
                refreshTab(NPAdapter.NP_TAB_REMINDER);
                Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#0000FF'>" +
                        getString(R.string.app_name) + "</font>"));

                // Send broadcast Intent to widget(s) to refresh!
                sendBroadcastToWidget(this);
            }

            // If there is change in sankalpam preferences, then refresh location & the fragments.
            String sankalpamType = readPrefSankalpamType(this);
            if (!prefSankalpamType.equals(sankalpamType)) {
                prefSankalpamType = sankalpamType;
                refreshTab(NPAdapter.NP_TAB_SANKALPAM);
                // No need to inform widget here for Sankalpam change!
            }

            int selectedAyanamsa = readPrefAyanamsaSelection(this);
            if (prefAyanamsa != selectedAyanamsa) {
                prefAyanamsa = selectedAyanamsa;
                refreshPanchangamDetails();

                // Send broadcast Intent to widget(s) to refresh!
                sendBroadcastToWidget(this);
            }

            int selectedChaandramanamType = readPrefChaandramanaType(this);
            if (prefChaandramanamType != selectedChaandramanamType) {
                prefChaandramanamType = selectedChaandramanamType;
                refreshPanchangamDetails();

                // Send broadcast Intent to widget(s) to refresh!
                sendBroadcastToWidget(this);
            }
        } else if (requestCode == CALENDAR_REQUEST_CODE) {
            if (data != null) {
                int calYear = data.getIntExtra("Calendar_Year", 0);
                int calMonth = data.getIntExtra("Calendar_Month", 0);
                int calDate = data.getIntExtra("Calendar_Date", 0);
                vedicCalendar.setDate(calDate, calMonth, calYear, 0, 0);
                refreshTab(NPAdapter.NP_TAB_PANCHANGAM);
                refreshTab(NPAdapter.NP_TAB_SANKALPAM);

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
        int defPanchangamType = VedicCalendar.PANCHANGAM_TYPE_DRIK_GANITHAM;
        String defPanchangamTypeStr = context.getString(R.string.pref_def_panchangam);
        SharedPreferences localPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (localPreferences != null) {
            defPanchangamTypeStr = localPreferences.getString(SettingsFragment.PREF_PANCHANGAM_KEY, defPanchangamTypeStr);

            if (defPanchangamTypeStr.equalsIgnoreCase(
                    context.getString(R.string.pref_panchangam_telugu_panchangam))) {
                defPanchangamType = VedicCalendar.PANCHANGAM_TYPE_TELUGU_PANCHANGAM;
            } else if (defPanchangamTypeStr.equalsIgnoreCase(
                    context.getString(R.string.pref_panchangam_kannada_panchangam))) {
                defPanchangamType = VedicCalendar.PANCHANGAM_TYPE_KANNADA_PANCHANGAM;
            }
        }

        return defPanchangamType;
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
        placesTimezoneDB.put("Chennai, India", new PlacesInfo(80.2707, 13.0827, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Mumbai, India", new PlacesInfo(72.8777, 19.0760, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kolkata, India", new PlacesInfo(88.3639, 22.5726, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("New Delhi, India", new PlacesInfo(77.2090, 28.6139, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Tirunelveli, India", new PlacesInfo(77.7567, 8.7139, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Salem, India", new PlacesInfo(78.1460, 11.6643, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Chidambaram, India", new PlacesInfo(79.6912, 11.4070, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Tenkasi, India", new PlacesInfo(77.3161, 8.9594, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kumbakonam, India", new PlacesInfo(79.3845, 10.9602, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Coimbatore, India", new PlacesInfo(76.9558, 11.0168, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Thanjavur, India", new PlacesInfo(79.1378, 10.7870, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kancheepuram, India", new PlacesInfo(79.6947, 12.8185, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Erode, India", new PlacesInfo(77.7172, 11.3410, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Tiruvannamalai, India", new PlacesInfo(79.0747, 12.2253, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Manali, India", new PlacesInfo(77.1892, 32.2432, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Shimla, India", new PlacesInfo(77.1734, 31.1048, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Dehradun, India", new PlacesInfo(78.0322, 30.3165, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kedarnath, India", new PlacesInfo(79.0669, 30.7346, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Badrinath, India", new PlacesInfo(79.4938, 30.7433, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Lucknow, India", new PlacesInfo(80.9462, 26.8467, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Allahabad, India", new PlacesInfo(81.8463, 25.4358, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ayodhya, India", new PlacesInfo(82.1998, 26.7922, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Haridwar, India", new PlacesInfo(78.1642, 29.9457, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Rishikesh, India", new PlacesInfo(78.2676, 30.0869, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Varanasi, India", new PlacesInfo(82.9739, 25.3176, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Gaya, India", new PlacesInfo(85.0002, 24.7914, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Patna, India", new PlacesInfo(85.1376, 25.5941, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ranchi, India", new PlacesInfo(85.3096, 23.3441, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bodhgaya, India", new PlacesInfo(84.9870, 24.6961, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kanpur, India", new PlacesInfo(80.3319, 26.4499, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Jammu, India", new PlacesInfo(74.8570, 32.7266, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Srinagar, India", new PlacesInfo(74.7973, 34.0837, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ladakh, India", new PlacesInfo(77.5619, 34.2268, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kargil, India", new PlacesInfo(76.1349, 34.5539, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Nagpur, India", new PlacesInfo(79.0882, 21.1458, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Shirdi, India", new PlacesInfo(74.4762, 19.7645, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Nashik, India", new PlacesInfo(73.7898, 19.9975, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Pune, India", new PlacesInfo(73.8567, 18.5204, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kolhapur, India", new PlacesInfo(74.2433, 16.7050, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Latur, India", new PlacesInfo(76.5604, 18.4088, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Calicut, India", new PlacesInfo(75.7804, 11.2588, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Cochin, India", new PlacesInfo(76.2673, 9.9312, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Thiruvananthapuram, India", new PlacesInfo(76.9366, 8.5241, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Palaghat, India", new PlacesInfo(76.6548, 10.7867, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bengaluru, India", new PlacesInfo(77.5946, 12.9716, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Mangalore, India", new PlacesInfo(74.8560, 12.9141, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Belgaum, India", new PlacesInfo(74.4977, 15.8497, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Udipi, India", new PlacesInfo(74.7421, 13.3409, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Sringeri, India", new PlacesInfo(75.2567, 13.4198, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bagalkot, India", new PlacesInfo(75.6615, 16.1691, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Gulbarga, India", new PlacesInfo(76.8343, 17.3297, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Hyderabad, India", new PlacesInfo(78.4867, 17.3850, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Srisailam, India", new PlacesInfo(78.8687, 16.0733, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Vishakapatnam, India", new PlacesInfo(83.2185, 17.6868, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Surat, India", new PlacesInfo(72.8311, 21.1702, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Thane, India", new PlacesInfo(72.9781, 19.2183, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Indore, India", new PlacesInfo(75.8577, 22.7196, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bhopal, India", new PlacesInfo(77.4126, 23.2599, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Vadodara, India", new PlacesInfo(73.1812, 22.3072, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ghaziabad, India", new PlacesInfo(77.4538, 28.6692, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ludhiana, India", new PlacesInfo(75.8573, 30.9010, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Agra, India", new PlacesInfo(78.0081, 27.1767, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Meerut, India", new PlacesInfo(77.7064, 28.9845, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Rajkot, India", new PlacesInfo(70.8022, 22.3039, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Aurangabad, India", new PlacesInfo(75.3433, 19.8762, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ahmedabad, India", new PlacesInfo(72.5714, 23.0225, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Jaipur, India", new PlacesInfo(75.7873, 26.9124, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Dhanbad, India", new PlacesInfo(86.4304, 23.7957, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Amritsar, India", new PlacesInfo(74.8723, 31.6340, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Navi Mumbai, India", new PlacesInfo(73.0297, 19.0330, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kalyan-Dombivli, India", new PlacesInfo(73.1305, 19.2403, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Faridabad, India", new PlacesInfo(77.3178, 28.4089, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Vasai-Virar, India", new PlacesInfo(72.8397, 19.3919, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Howrah, India", new PlacesInfo(88.2636, 22.5958, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Jabalpur, India", new PlacesInfo(79.9864, 23.1815, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Madurai, India", new PlacesInfo(78.1198, 9.9252, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Vijayawada, India", new PlacesInfo(80.6480, 16.5062, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Jodhpur, India", new PlacesInfo(73.0243, 26.2389, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Raipur, India", new PlacesInfo(81.6296, 21.2514, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kota, India", new PlacesInfo(75.8648, 25.2138, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Chandigarh, India", new PlacesInfo(76.7794, 30.7333, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Guwahati, India", new PlacesInfo(91.7362, 26.1445, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Solapur, India", new PlacesInfo(75.9064, 17.6599, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Hubli–Dharwad, India", new PlacesInfo(75.1240, 15.3647, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Mysore, India", new PlacesInfo(76.6394, 12.2958, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Tiruchirappalli, India", new PlacesInfo(78.7047, 10.7905, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bareilly, India", new PlacesInfo(79.4304, 28.3670, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Aligarh, India", new PlacesInfo(78.0880, 27.8974, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Tiruppur, India", new PlacesInfo(77.3411, 11.1085, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Gurgaon, India", new PlacesInfo(77.0266, 28.4595, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Moradabad, India", new PlacesInfo(78.7733, 28.8386, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Jalandhar, India", new PlacesInfo(75.5762, 31.3260, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bhubaneswar, India", new PlacesInfo(85.8245, 20.2961, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Warangal, India", new PlacesInfo(79.5941, 17.9689, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Mira-Bhayandar, India", new PlacesInfo(72.8544, 19.2952, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Jalgaon, India", new PlacesInfo(75.5626, 21.0077, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Guntur, India", new PlacesInfo(80.4365, 16.3067, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bhiwandi, India", new PlacesInfo(73.0483, 19.2813, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Saharanpur, India", new PlacesInfo(77.5552, 29.9680, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Gorakhpur, India", new PlacesInfo(83.3732, 26.7606, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bikaner, India", new PlacesInfo(73.3119, 28.0229, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Amravati, India", new PlacesInfo(77.7523, 20.9320, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Noida, India", new PlacesInfo(77.3910, 28.5355, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Jamshedpur, India", new PlacesInfo(86.2029, 22.8046, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bhilai, India", new PlacesInfo(81.3509, 21.1938, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Cuttack, India", new PlacesInfo(85.8830, 20.4625, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Firozabad, India", new PlacesInfo(78.3957, 27.1592, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kochi, India", new PlacesInfo(76.2673, 9.9312, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Nellore, India", new PlacesInfo(79.9865, 14.4426, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bhavnagar, India", new PlacesInfo(72.1519, 21.7645, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Durgapur, India", new PlacesInfo(87.3119, 23.5204, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Asansol, India", new PlacesInfo(86.9661, 23.6889, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Rourkela, India", new PlacesInfo(84.8536, 22.2604, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Nanded, India", new PlacesInfo(77.3210, 19.1383, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ajmer, India", new PlacesInfo(74.6399, 26.4499, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Akola, India", new PlacesInfo(77.0082, 20.7002, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Jamnagar, India", new PlacesInfo(70.0577, 22.4707, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ujjain, India", new PlacesInfo(75.7885, 23.1765, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Loni, India", new PlacesInfo(77.2986, 28.7334, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Siliguri, India", new PlacesInfo(88.3953, 26.7271, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Jhansi, India", new PlacesInfo(78.5685, 25.4484, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ulhasnagar, India", new PlacesInfo(73.1645, 19.2215, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ambattur, India", new PlacesInfo(80.1548, 13.1143, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Rajahmundry, India", new PlacesInfo(81.8040, 17.0005, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Malegaon, India", new PlacesInfo(74.5089, 20.5579, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kurnool, India", new PlacesInfo(78.0373, 15.8281, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Tirupati, India", new PlacesInfo(79.4192, 13.6288, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Tirumala, India", new PlacesInfo(79.3509, 13.6288, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Udaipur, India", new PlacesInfo(73.7125, 24.5854, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kakinada, India", new PlacesInfo(82.2475, 16.9891, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Davanagere, India", new PlacesInfo(75.9218, 14.4644, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kozhikode, India", new PlacesInfo(75.7804, 11.2588, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bokaro, India", new PlacesInfo(86.1511, 23.6693, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("South Dumdum, India", new PlacesInfo(88.3983, 22.6089, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bellary, India", new PlacesInfo(76.9214, 15.1394, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Patiala, India", new PlacesInfo(76.3869, 30.3398, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Gopalpur, India", new PlacesInfo(84.8620, 19.2647, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Agartala, India", new PlacesInfo(91.2868, 23.8315, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bhagalpur, India", new PlacesInfo(86.9842, 25.2425, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Muzaffarnagar, India", new PlacesInfo(77.7085, 29.4727, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bhatpara, India", new PlacesInfo(88.3912, 22.8536, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Panihati, India", new PlacesInfo(88.4037, 22.6939, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Dhule, India", new PlacesInfo(74.7749, 20.9042, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Rohtak, India", new PlacesInfo(76.6066, 28.8955, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Sagar, India", new PlacesInfo(78.7378, 23.8388, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Korba, India", new PlacesInfo(82.7501, 22.3595, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bhilwara, India", new PlacesInfo(74.6313, 25.3407, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Brahmapur, India", new PlacesInfo(84.7941, 19.3150, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Muzaffarpur, India", new PlacesInfo(85.3910, 26.1197, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ahmednagar, India", new PlacesInfo(74.7480, 19.0948, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Mathura, India", new PlacesInfo(77.6737, 27.4924, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kollam, India", new PlacesInfo(76.6141, 8.8932, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Avadi, India", new PlacesInfo(80.0970, 13.1067, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kadapa, India", new PlacesInfo(78.8242, 14.4673, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Anantapuram, India", new PlacesInfo(77.6006, 14.6819, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kamarhati, India", new PlacesInfo(88.3706, 22.6847, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bilaspur, India", new PlacesInfo(82.1409, 22.0797, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Sambalpur, India", new PlacesInfo(83.9812, 21.4669, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Satara, India", new PlacesInfo(74.0183, 17.6805, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bijapur, India", new PlacesInfo(75.7100, 16.8302, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Rampur, India", new PlacesInfo(79.0220, 28.7983, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Shimoga, India", new PlacesInfo(75.5681, 13.9299, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Chandrapur, India", new PlacesInfo(79.2961, 19.9615, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Junagadh, India", new PlacesInfo(70.4579, 21.5222, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Thrissur, India", new PlacesInfo(76.2144, 10.5276, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Alwar, India", new PlacesInfo(76.6346, 27.5530, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bardhaman, India", new PlacesInfo(87.8615, 23.2324, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Parbhani, India", new PlacesInfo(76.7748, 19.2608, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Tumkur, India", new PlacesInfo(77.1173, 13.3379, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Khammam, India", new PlacesInfo(80.1514, 17.2473, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Uzhavarkarai, India", new PlacesInfo(79.7733, 11.9394, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Panipat, India", new PlacesInfo(76.9635, 29.3909, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Darbhanga, India", new PlacesInfo(85.8918, 26.1542, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Aizawl, India", new PlacesInfo(92.7173, 23.7307, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Dewas, India", new PlacesInfo(76.0534, 22.9676, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ichalkaranji, India", new PlacesInfo(74.4593, 16.6886, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Karnal, India", new PlacesInfo(76.9905, 29.6857, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bathinda, India", new PlacesInfo(74.9455, 30.2110, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Jalna, India", new PlacesInfo(75.8816, 19.8347, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Eluru, India", new PlacesInfo(81.0952, 16.7107, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Barasat, India", new PlacesInfo(88.4789, 22.7248, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Purnia, India", new PlacesInfo(87.4753, 25.7771, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Puri, India", new PlacesInfo(85.8312, 19.8135, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Satna, India", new PlacesInfo(80.8322, 24.6005, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Sonipat, India", new PlacesInfo(77.0151, 28.9931, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Imphal, India", new PlacesInfo(93.9368, 24.8170, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ratlam, India", new PlacesInfo(75.0367, 23.3315, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Anantapur, India", new PlacesInfo(77.6006, 14.6819, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Karimnagar, India", new PlacesInfo(79.1288, 18.4386, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ambarnath, India", new PlacesInfo(73.1926, 19.1825, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("North Dumdum, India", new PlacesInfo(88.4090, 22.6626, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bharatpur, India", new PlacesInfo(77.5030, 27.2152, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Begusarai, India", new PlacesInfo(86.1272, 25.4182, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Gandhidham, India", new PlacesInfo(70.1337, 23.0753, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Tiruvottiyur, India", new PlacesInfo(80.3001, 13.1643, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Pondicherry, India", new PlacesInfo(79.8083, 11.9416, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Sikar, India", new PlacesInfo(75.1398, 27.6094, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Thoothukudi, India", new PlacesInfo(78.1348, 8.7642, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Rewa, India", new PlacesInfo(81.3037, 24.5362, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Pali, India", new PlacesInfo(73.3311, 25.7781, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Raichur, India", new PlacesInfo(77.3566, 16.2160, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ramagundam, India", new PlacesInfo(79.5134, 18.7519, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Rameswaram, India", new PlacesInfo(79.3129, 9.2876, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Silchar, India", new PlacesInfo(92.7789, 24.8333, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Vijayanagaram, India", new PlacesInfo(83.3956, 18.1067, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Tenali, India", new PlacesInfo(80.6444, 16.2379, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Nagercoil, India", new PlacesInfo(77.4119, 8.1833, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Sri Ganganagar, India", new PlacesInfo(73.8800, 29.9094, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Sambhal, India", new PlacesInfo(78.5718, 28.5904, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Mango, India", new PlacesInfo(86.2294, 22.8384, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Nadiad, India", new PlacesInfo(72.8634, 22.6916, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Secunderabad, India", new PlacesInfo(78.4983, 17.4399, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Naihati, India", new PlacesInfo(88.4220, 22.8895, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Yamunanagar, India", new PlacesInfo(77.2674, 30.1290, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Pallavaram, India", new PlacesInfo(80.1491, 12.9675, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Dindigul, India", new PlacesInfo(77.9695, 10.3624, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kharagpur, India", new PlacesInfo(87.2320, 22.3460, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Hospet, India", new PlacesInfo(76.3909, 15.2689, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Gandhinagar, India", new PlacesInfo(72.6369, 23.2156, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ongole, India", new PlacesInfo(80.0499, 15.4777, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Nandyal, India", new PlacesInfo(78.4873, 13.0827, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bhiwani, India", new PlacesInfo(76.1322, 28.7975, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ambala, India", new PlacesInfo(76.7821, 30.3752, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Chittoor, India", new PlacesInfo(79.1003, 13.2172, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Vellore, India", new PlacesInfo(79.1325, 12.9165, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Alappuzha, India", new PlacesInfo(76.3388, 9.4981, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kottayam, India", new PlacesInfo(76.5222, 9.5916, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Machilipatnam, India", new PlacesInfo(81.1303, 16.1809, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Hindupur, India", new PlacesInfo(77.5009, 13.8223, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Udupi, India", new PlacesInfo(74.7421, 13.3409, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Port Blair, India", new PlacesInfo(92.7265, 11.6234, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bhimavaram, India", new PlacesInfo(81.5212, 16.5449, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Madanapalle, India", new PlacesInfo(78.5010, 13.5560, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Guntakal, India", new PlacesInfo(77.3770, 15.1661, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Dharmavaram, India", new PlacesInfo(77.7201, 14.4125, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Srikakulam, India", new PlacesInfo(83.8938, 18.2949, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Pudukkottai, India", new PlacesInfo(78.8001, 10.3833, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Hosur, India", new PlacesInfo(77.8253, 12.7409, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Tadipatri, India", new PlacesInfo(78.0092, 14.9091, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Karaikudi, India", new PlacesInfo(78.7803, 10.0763, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Gangtok, India", new PlacesInfo(88.6138, 27.3314, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kavaratti, India", new PlacesInfo(72.6358, 10.5593, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kanyakumari, India", new PlacesInfo(77.5385, 8.0883, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Nagapattinam, India", new PlacesInfo(79.8449, 10.7672, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Pollachi, India", new PlacesInfo(77.0048, 10.6609, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Chettinad, India", new PlacesInfo(78.7773, 10.1606, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Courtallam, India", new PlacesInfo(77.2779, 8.9341, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Mahabalipuram, India", new PlacesInfo(80.1945, 12.6208, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Ooty, India", new PlacesInfo(76.6950, 11.4102, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kodaikanal, India", new PlacesInfo(77.4892, 10.2381, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Coonoor, India", new PlacesInfo(76.7959, 11.3530, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Yelagiri, India", new PlacesInfo(78.6345, 12.5856, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Yercaud, India", new PlacesInfo(78.2097, 11.7748, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kotagiri, India", new PlacesInfo(76.8617, 11.4218, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Valparai, India", new PlacesInfo(76.9554, 10.3270, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Theni, India", new PlacesInfo(77.4768, 10.0104, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Vedanthangal, India", new PlacesInfo(79.8561, 12.5455, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Palani, India", new PlacesInfo(77.5161, 10.4500, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Mayiladuthurai, India", new PlacesInfo(79.6526, 11.1018, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Thiruvarur, India", new PlacesInfo(79.6344, 10.7661, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Bodinayakkanur, India", new PlacesInfo(77.3497, 10.0106, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Mudumalai, India", new PlacesInfo(76.5257, 11.6376, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Dharmapuri, India", new PlacesInfo(78.1582, 12.1211, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Nilgiris, India", new PlacesInfo(76.7337, 11.4916, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Dhanushkodi, India", new PlacesInfo(79.4183, 9.1794, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Krishnagiri, India", new PlacesInfo(78.2150, 12.5266, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Auroville, India", new PlacesInfo(79.8069, 12.0052, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kolli Hills, India", new PlacesInfo(78.3387, 11.2485, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Thiruttani, India", new PlacesInfo(79.6117, 13.1746, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Namakkal, India", new PlacesInfo(78.1674, 11.2189, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Papanasam, India", new PlacesInfo(79.2864, 10.9233, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kalancheri, India", new PlacesInfo(79.2687, 10.8144, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Kalladaikurichi, India", new PlacesInfo(77.4651, 8.6830, INDIAN_STANDARD_TIME));
        placesTimezoneDB.put("Accra, Ghana", new PlacesInfo(-0.1870, 5.6037, 0.0));
        placesTimezoneDB.put("Lisbon, Portugal", new PlacesInfo(-9.1393, 38.7223, 1.0));
        placesTimezoneDB.put("Dublin, Ireland", new PlacesInfo(-6.2603, 53.3498, 1.0));
        placesTimezoneDB.put("London, UK", new PlacesInfo(-0.1278, 51.5074, 1.0));
        placesTimezoneDB.put("Oslo, Norway", new PlacesInfo(10.7522, 59.9139, 2.0));
        placesTimezoneDB.put("Stockholm, Sweden", new PlacesInfo(18.0686, 59.3293, 2.0));
        placesTimezoneDB.put("Helsinki, Finland", new PlacesInfo(24.9384, 60.1699, 1.0));
        placesTimezoneDB.put("Madrid, Spain", new PlacesInfo(-3.7038, 40.4168, 2.0));
        placesTimezoneDB.put("Paris, France", new PlacesInfo(2.3522, 48.8566, 2.0));
        placesTimezoneDB.put("Frankfurt, Germany", new PlacesInfo(8.6821, 50.1109, 2.0));
        placesTimezoneDB.put("Munich, Germany", new PlacesInfo(11.5820, 48.1351, 2.0));
        placesTimezoneDB.put("Vienna, Austria", new PlacesInfo(16.3738, 48.2082, 2.0));
        placesTimezoneDB.put("Rome, Italy", new PlacesInfo(12.4964, 41.9028, 2.0));
        placesTimezoneDB.put("Venice, Italy", new PlacesInfo(12.3155, 45.4408, 2.0));
        placesTimezoneDB.put("Zurich, Switzerland", new PlacesInfo(8.5417, 47.3769, 2.0));
        placesTimezoneDB.put("Bern, Switzerland", new PlacesInfo(7.4474, 46.9480, 2.0));
        placesTimezoneDB.put("Moosseedorf, Switzerland", new PlacesInfo(7.4846, 47.0146, 2.0));
        placesTimezoneDB.put("Brussels, Belgium", new PlacesInfo(4.3517, 50.8503, 2.0));
        placesTimezoneDB.put("Warsaw, Poland", new PlacesInfo(21.0122, 52.2297, 2.0));
        placesTimezoneDB.put("Prague, Czech Republic", new PlacesInfo(14.4378, 50.0755, 2.0));
        placesTimezoneDB.put("Budapest, Hungary", new PlacesInfo(19.0402, 47.4979, 2.0));
        placesTimezoneDB.put("Cairo, Egypt", new PlacesInfo(31.2357, 30.0444, 2.0));
        placesTimezoneDB.put("Johannesburg, South Africa", new PlacesInfo(28.0473, -26.2041, 2.0));
        placesTimezoneDB.put("Durban, South Africa", new PlacesInfo(31.0218, -29.8587, 2.0));
        placesTimezoneDB.put("Lusaka, Zambia", new PlacesInfo(28.3228, -15.3875, 2.0));
        placesTimezoneDB.put("Harare, Zimbabwe", new PlacesInfo(31.0492, -17.8216, 2.0));
        placesTimezoneDB.put("Mogadishu, Somalia", new PlacesInfo(45.3182, 2.0469, 3.0));
        placesTimezoneDB.put("Nairobi, Kenya", new PlacesInfo(36.8219, -1.2921, 3.0));
        placesTimezoneDB.put("Bucharest, Romania", new PlacesInfo(26.1025, 44.4268, 3.0));
        placesTimezoneDB.put("Athens, Greece", new PlacesInfo(23.7275, 37.9838, 3.0));
        placesTimezoneDB.put("Moscow, Russia", new PlacesInfo(37.6173, 55.7558, 3.0));
        placesTimezoneDB.put("Ankara, Turkey", new PlacesInfo(32.8597, 39.9334, 3.0));
        placesTimezoneDB.put("Baghdad, Iraq", new PlacesInfo(44.3661, 33.3152, 3.0));
        placesTimezoneDB.put("Jerusalem, Israel", new PlacesInfo(35.2137, 31.7683, 3.0));
        placesTimezoneDB.put("Riyadh, Saudi Arabia", new PlacesInfo(46.6753, 24.7136, 3.0));
        placesTimezoneDB.put("Doha, Qatar", new PlacesInfo(51.5310, 25.2854, 3.0));
        placesTimezoneDB.put("Kuwait, Kuwait", new PlacesInfo(47.4818, 29.3117, 3.0));
        placesTimezoneDB.put("Dubai, United Arab Emirates", new PlacesInfo(55.2708, 25.2048, 4.0));
        placesTimezoneDB.put("Abu Dhabi, United Arab Emirates", new PlacesInfo(54.3773, 24.4539, 4.0));
        placesTimezoneDB.put("Muscat, Oman", new PlacesInfo(58.3829, 23.5880, 4.0));
        placesTimezoneDB.put("Tehran, Iran", new PlacesInfo(51.3890, 35.6892, 4.5));
        placesTimezoneDB.put("Kabul, Afganistan", new PlacesInfo(69.2075, 34.5553, 4.5));
        placesTimezoneDB.put("Columbo, Srilanka", new PlacesInfo(79.8612, 6.9271, 5.5));
        placesTimezoneDB.put("Islamabad, Pakistan", new PlacesInfo(73.0479, 33.6844, 5.0));
        placesTimezoneDB.put("Dhaka, Bangladesh", new PlacesInfo(90.4125, 23.8103, 6.0));
        placesTimezoneDB.put("Jakarta, Indonesia", new PlacesInfo(106.8456, -6.2088, 7.0));
        placesTimezoneDB.put("Taipei, Taiwan", new PlacesInfo(121.5654, 25.0330, 8.0));
        placesTimezoneDB.put("Singapore", new PlacesInfo(103.8198, 1.3521, 8.0));
        placesTimezoneDB.put("Hongkong", new PlacesInfo(114.1694, 22.3193, 8.0));
        placesTimezoneDB.put("Beijing, China", new PlacesInfo(116.4074, 39.9042, 8.0));
        placesTimezoneDB.put("Perth, Australia", new PlacesInfo(115.8613, -31.9523, 8.0));
        placesTimezoneDB.put("Tokyo, Japan", new PlacesInfo(139.6503, 35.6762, 9.0));
        placesTimezoneDB.put("Kawasaki, Japan", new PlacesInfo(139.7029, 35.5308, 9.0));
        placesTimezoneDB.put("Kyoto, Japan", new PlacesInfo(135.7681, 35.0116, 9.0));
        placesTimezoneDB.put("Matsumoto, Japan", new PlacesInfo(137.9720, 36.2380, 9.0));
        placesTimezoneDB.put("Seoul, South Korea", new PlacesInfo(126.9780, 37.5665, 9.0));
        placesTimezoneDB.put("Adelaide, Australia", new PlacesInfo(138.6007, -34.9285, 9.5));
        placesTimezoneDB.put("Melbourne, Australia", new PlacesInfo(144.9631, -37.8136, 10.0));
        placesTimezoneDB.put("Sydney, Australia", new PlacesInfo(151.2093, -33.8688, 10.0));
        placesTimezoneDB.put("Canberra, Australia", new PlacesInfo(149.1300, -35.2809, 10.0));
        placesTimezoneDB.put("Brisbane, Australia", new PlacesInfo(153.0260, -27.4705, 10.0));
        placesTimezoneDB.put("Hobart, Australia", new PlacesInfo(147.3257, -42.8826, 10.0));
        placesTimezoneDB.put("Christchurch, New Zealand", new PlacesInfo(172.6306, -43.5320, 12.0));
        placesTimezoneDB.put("Wellington, New Zealand", new PlacesInfo(174.7787, -41.2924, 12.0));
        placesTimezoneDB.put("San Francisco, USA", new PlacesInfo(-122.4194, 37.7749, -7.0));
        placesTimezoneDB.put("San Jose, USA", new PlacesInfo(-121.8863, 37.3382, -7.0));
        placesTimezoneDB.put("Fremont, USA", new PlacesInfo(-121.9886, 37.5485, -7.0));
        placesTimezoneDB.put("Las Vegas, USA", new PlacesInfo(-115.1398, 36.1699, -7.0));
        placesTimezoneDB.put("Phoenix, USA", new PlacesInfo(-112.0740, 33.4484, -7.0));
        placesTimezoneDB.put("Seattle, USA", new PlacesInfo(-122.3321, 47.6062, -7.0));
        placesTimezoneDB.put("Winnipeg, Canada", new PlacesInfo(-97.1384, 49.8951, -6.0));
        placesTimezoneDB.put("Toronto, Canada", new PlacesInfo(-79.3832, 43.6532, -4.0));
        placesTimezoneDB.put("Kansas, USA", new PlacesInfo(-98.4842, 39.0119, -5.0));
        placesTimezoneDB.put("Ann Arbor, USA", new PlacesInfo(-83.7430, 42.2808, -5.0));
        placesTimezoneDB.put("St Augustine, USA", new PlacesInfo(-81.3124, 29.9012, -4.0));
        placesTimezoneDB.put("Chester Springs, USA", new PlacesInfo(-75.6343, 40.0784, -4.0));
        placesTimezoneDB.put("New Jersey, USA", new PlacesInfo(-74.4057, 40.0583, -5.0));
        placesTimezoneDB.put("Mexico city, Mexico", new PlacesInfo(-99.1332, 19.4326, -5.0));
        placesTimezoneDB.put("Chicago, USA", new PlacesInfo(-87.6298, 41.8781, -5.0));
        placesTimezoneDB.put("Richmond, USA", new PlacesInfo(-77.4360, 37.5407, -4.0));
        placesTimezoneDB.put("New York, USA", new PlacesInfo(-74.0060, 40.7128, -4.0));
        placesTimezoneDB.put("Washington, USA", new PlacesInfo(-120.7401, 47.7511, -4.0));
        placesTimezoneDB.put("Boston", new PlacesInfo(-71.0589, 42.3601, -4.0));
        placesTimezoneDB.put("Miami, USA", new PlacesInfo(-80.1918, 25.7617, -4.0));
        placesTimezoneDB.put("Tampa Bay, USA", new PlacesInfo(-82.5437, 27.7634, -4.0));
        placesTimezoneDB.put("Jacksonville, USA", new PlacesInfo(-81.6557, 30.3322, -4.0));
        placesTimezoneDB.put("Ottawa, Canada", new PlacesInfo(-75.6972, 45.4215, -4.0));
        placesTimezoneDB.put("Salem, USA", new PlacesInfo(-70.8967, 42.5195, -4.0));
        placesTimezoneDB.put("Buenos Aires, Argentina", new PlacesInfo(-58.3816, -34.6037, -3.0));
        placesTimezoneDB.put("Rio de Janeiro, Brazil", new PlacesInfo(-43.1729, -22.9068, -3.0));

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

    public static HashMap<String, String[]> buildVedicCalendarLocaleList(Context context) {
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
            arrayList = context.getResources().getStringArray(R.array.thithi_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_THITHI, arrayList);

            // Step7: Sankalpa Thithi
            arrayList = context.getResources().getStringArray(R.array.sankalpa_thithi_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_SANKALPA_THITHI, arrayList);

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
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_DHINAM, arrayList);

            // Step15: Horai
            arrayList = context.getResources().getStringArray(R.array.horai_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_HORAI, arrayList);

            // Step16: Amruthathi Yogam
            arrayList = context.getResources().getStringArray(R.array.amruthathi_yogam_list);
            vedicCalendarLocaleList.put(VedicCalendar.VEDIC_CALENDAR_TABLE_TYPE_AMRUTATHI_YOGAM, arrayList);
        }

        return vedicCalendarLocaleList;
    }

    public static void sendBroadcastToWidget(Context context) {
        Intent intent = new Intent(context, NithyaPanchangamWidget.class);
        intent.putExtra(NP_UPDATE_WIDGET, "Update");
        context.sendBroadcast(intent);
    }
}