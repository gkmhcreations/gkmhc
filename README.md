# Nithya Panchangam Android App
# GKM Heritage Creations

1) App Details:
All the information related to App can be read in below link:
https://github.com/gkmhcreations/gkmhc/wiki/Nithya-Panchangam

2) License
This whole software project is distributed under GNU GPL:
 - http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 
Use of this software as a whole or in parts to copy, modify, redistribute shall be in accordance with terms & conditions in GNU GPL license.

3) Development View

Following features are supported in this App:
 A) Main Features represented via 5 tabs:
   1) Panchangam
      - Panchangam.java - This file contains the functions that handle panchangam fragment.
   2) Sankalpam
      - Sankalpam.java - This file contains the functions that handle Sankalpam fragment.
   3) Alarm
      - Alarm.java            - This file contains functions that are associated with Alarms
                                functions like Start, Stop, Restart and also to handle the UI.
      - AlarmAdapter.java     - This file contains functions that displays Alarms in a ListView
      - AlarmViewHolder.java  - A view holder that holds Alarm information
      - HandleAlarmReminderActivity.java - A common activity to add/modify Alarms and Reminders.
   4) Reminder
      - Reminder.java           - This file contains functions that are associated with Reminder
                                  functions like Start, Stop, Restart and also to handle the UI.
      - ReminderAdapter.java    - This file contains functions that displays Reminders in a ListView.
      - ReminderViewHolder.java - A view holder that holds Reminder information
   5) StopWatch
      - StopWatch.java - This file contains the functions that handle StopWatch fragment.

 B) Widget
    - NithyaPanchangamWidget.java - This file contains functions that handle widget for the App.

 C) Settings
   Nithya Panchangam allows the following settings to be configured by user
   - Locale               - Sanskrit / Tamil / English
   - Panchangam Type      - Only "Drik Ganitham" is supported.
                            TODO - Vakhyam to be added in later versions.
   - Location Type        - Manual / GPS
   - Sankalpam Type       - Shubham / Srartham
   All of these are handled in below files:
   - NithyaPanchangamSettings - This file contains the functions that handle Settings Activity
                                to read/write Nithya Panchangam settings information to/from a
                                persistent storage (SharedPreferences).
   - SettingsFragment.java    - This file contains the functions that handle Settings fragment.

 D) Support/Utility Functions
   1) Vedic Calendar
      - VedicCalendar.java - This file contains the generic functions that does all the
                             Panchangam & Sankalpam calculations and exposes a simple-to-use
                             interface(API) for the Apps.
                             SwissEph library is used for all solar & lunar calculations.
                             Note: This is reusable across different platforms.
                             TODO - Vakhyam to be added in later versions.
   2) Broadcast Receiver
      - NPBroadcastReceiver.java - This file contains functions that handles all lifecycles
                                   event related to Alarms & Reminders.
   3) Adapter
      - NPAdapter.java - This file contains functions that handle create/display information
                         related to all the 5 tabs in this App.
   4) NPDB
      - NPDB.java - This file contains generic interfaces to read/write Alarms & Reminders
                    to/from a persistent storage(SQLite).
   5) Lock Screen Notification
      - AlarmLockScreenNotification.java - This file display a fullscreen Activity when an Alarm
                                           fires but Phone's display is locked.
   6) Monthly Calendar
      - NithyaPanchangamCalendar.java - This file contains functions that displays the Monthly
                                        calendar and handles day/month/year selections as well.
      - CalendarAdapter.java          - This file contains functions that handle each cell in a
                                        calendar in the form of a RecyclerView.
      - CalendarViewHolder.java       - A view holder that holds "Dhina" information
   7) Splash
      - SplashScreen.java     - This file contains simple functions to load an activity
                                at the start of NithyaPanchangam Activity launch.
   8) About
      - AboutActivity.java    - This file contains functions to display information about the App.
   9) Copy SwissEph Asset Files
      - CopyToAssets.java   - This file contains functions that handle copying SwissEph related
                                metadata information to the App's default location on the Phone.
