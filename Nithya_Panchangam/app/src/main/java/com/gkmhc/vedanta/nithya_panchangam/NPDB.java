package com.gkmhc.vedanta.nithya_panchangam;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Nithya Panchangam Database to store the following in persistent storage:
 * 1) Alarm Information:
 * {AlarmID, AlarmStatus, AlarmHour, AlarmMinute, AlarmRingTone, AlarmVibrate, AlarmRepeat, Label}
 *
 * 2) Reminder Information:
 * {ReminderID, AlarmStatus, AlarmHour, AlarmMinute, AlarmRingTone, AlarmVibrate, AlarmRepeat,
 *  Label, iconID}
 *
 * Three options to store in persistent DB:
 * 1) SQLite
 *    Pros: Scalable, Modular, Well-tested, Extensible
 *    Cons: Increased App-size, higher complexity to maintain limited Alarm Info
 * 2) App-specific file -
 *    Pros: Scalable, Quick to Deploy
 *    Cons: Increased App-size, Lot of testing needed, Complex file operations slows down App,
 *          higher complexity to maintain limited Alarm Info,
 * 3) SharedPreferences
 *    Pros: Lower App-size, Modular, Quick to Deploy, Low maintenance, Low complexity
 *    Cons: Lot of testing needed, Not scalable as storage space is limited!
 *
 * After due consideration, choosing option[1] for initial version of the App.
 * Reason: How many Alarms & Reminders would a user have on his/her phone?
 *         50? (Max)
 *
 *         Memory Consumption would be ~2.5KB to ~25KB (depending on the # of alarms & options)
 *             - Max Alarms: 50
 *             - Memory Requirement (in bytes, per Alarm) : 50(min) to (500) max
 *
 *         Performance:
 *             - Add Alarm to DB takes ~10 ms
 *             - Update Alarm to DB takes ~14 ms
 *             - Delete Alarm from DB takes ~20 ms
 *             - Retrieve all Alarms from DB takes ~40 ms for 10-12 Alarms
 *
 *         Note: Memory & Performance are rough calculations based on limited trials!
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class NPDB extends SQLiteOpenHelper {
    private static final int DB_RESULT_FAILURE          = -1;
    private static final String NP_ALARM_TABLE          = "NP_ALARM_TABLE";
    private static final String NP_REMINDER_TABLE       = "NP_REMINDER_TABLE";
    private static final String COLUMN_ALARM_ID         = "ID";
    private static final String COLUMN_ALARM_TYPE       = "TYPE";
    private static final String COLUMN_ALARM_STATE      = "STATE";
    private static final String COLUMN_ALARM_HOUROFDAY  = "HOUROFDAY";
    private static final String COLUMN_ALARM_MINUTE     = "MINUTE";
    private static final String COLUMN_ALARM_RINGTONE   = "RINGTONE";
    private static final String COLUMN_ALARM_VIBRATE    = "VIBRATE";
    private static final String COLUMN_ALARM_REPEAT     = "REPEAT";
    private static final String COLUMN_ALARM_LABEL      = "LABEL";
    private static final String COLUMN_ALARM_ICON_ID    = "ICON";
    private static final int NP_ALARM_DB_VER            = 1;
    private static final String NP_DB_NAME              = "gkmhc_np.db";

    // Table for storing Alarms
    private static final String NP_ALARMS_TABLE_CREATE =
            "CREATE TABLE " + NP_ALARM_TABLE + " (" +
                    COLUMN_ALARM_ID         + " INTEGER PRIMARY KEY, " +
                    COLUMN_ALARM_TYPE       + " BIT, " +
                    COLUMN_ALARM_STATE      + " BIT, " +
                    COLUMN_ALARM_HOUROFDAY  + " INTEGER, " +
                    COLUMN_ALARM_MINUTE     + " INTEGER, " +
                    COLUMN_ALARM_RINGTONE   + " TEXT NOT NULL, " +
                    COLUMN_ALARM_VIBRATE    + " BIT, " +
                    COLUMN_ALARM_REPEAT     + " INTEGER, " +
                    COLUMN_ALARM_LABEL      + " TEXT);";

    // Table for storing Reminders
    private static final String NP_REMINDERS_TABLE_CREATE =
            "CREATE TABLE " + NP_REMINDER_TABLE + " (" +
                    COLUMN_ALARM_ID         + " INTEGER PRIMARY KEY, " +
                    COLUMN_ALARM_TYPE       + " BIT, " +
                    COLUMN_ALARM_STATE      + " BIT, " +
                    COLUMN_ALARM_HOUROFDAY  + " INTEGER, " +
                    COLUMN_ALARM_MINUTE     + " INTEGER, " +
                    COLUMN_ALARM_RINGTONE   + " TEXT NOT NULL, " +
                    COLUMN_ALARM_VIBRATE    + " BIT, " +
                    COLUMN_ALARM_REPEAT     + " INTEGER, " +
                    COLUMN_ALARM_LABEL      + " TEXT, " +
                    COLUMN_ALARM_ICON_ID    + " INTEGER);";

    public NPDB(@Nullable Context context) {
        super(context, NP_DB_NAME, null, NP_ALARM_DB_VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("NPDB","Creating Table(" + NP_ALARM_TABLE + " & " +
                NP_REMINDERS_TABLE_CREATE + ")!");
        db.execSQL(NP_ALARMS_TABLE_CREATE);
        db.execSQL(NP_REMINDERS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + NP_ALARM_TABLE);
        onCreate(db);
        Log.i("NPDB","Upgrading Tables(" + NP_ALARM_TABLE + " & " +
                NP_REMINDERS_TABLE_CREATE + ") to new ver(" + NP_ALARM_DB_VER + ")!");
    }

    public static class AlarmInfo {
        public final int alarmID;
        public final boolean alarmType;
        public boolean isAlarmOn;
        public final int alarmHourOfDay;
        public final int alarmMin;
        public final String ringTone;
        public final boolean toVibrate;
        public final int repeatOption;
        public final String label;

        AlarmInfo(int alarmID, boolean alarmType, boolean isAlarmOn, int hour, int min,
                  String ringTonePath, boolean vibrateOption, int repeatOption, String label) {
            this.alarmID = alarmID;
            this.alarmType = alarmType;
            this.isAlarmOn = isAlarmOn;
            this.alarmHourOfDay = hour;
            this.alarmMin = min;
            this.ringTone = ringTonePath;
            this.toVibrate = vibrateOption;
            this.repeatOption = repeatOption;
            this.label = label;
        }
    }

    public static class ReminderInfo extends AlarmInfo {
        public final int iconID;

        ReminderInfo(int alarmID, boolean alarmType, boolean isAlarmOn, int hour, int min,
                     String ringTonePath, boolean vibrateOption, int repeatOption, String label,
                     int iconID) {
            super(alarmID, alarmType, isAlarmOn, hour, min, ringTonePath, vibrateOption,
                  repeatOption, label);
            this.iconID = iconID;
        }
    }

    /**
     * Use this API to add Alarm information to persistent DB.
     *
     * @param context           App Context
     * @param alarmID           Alarm ID
     * @param isAlarmOn         On/Off
     * @param alarmHourOfDay    Alarm Hour of Day (24-hour format)
     * @param alarmMin          Alarm Minutes
     * @param ringTone          Full path to Ringtone
     * @param toVibrate         True - Vibrate, False - Do NOT Vibrate
     * @param repeatOption      Once   - Buzz Alarm one-time
     *                          Daily  - Buzz Alarm daily at the Alarm Hour & Minute
     *                          Custom - Buzz Alarm at the Alarm Hour & Minute on selected days
     * @param label             Alarm Label
     */
    public static void addAlarmToDB(Context context, boolean alarmType, int alarmID,
                                    boolean isAlarmOn, int alarmHourOfDay, int alarmMin,
                                    String ringTone, boolean toVibrate, int repeatOption,
                                    String label) {
        //long startTime = System.nanoTime();
        //Log.i("NPDB","Adding Alarm(" + alarmID + ") to DB!");

        // Alarm DB Record:
        // Key: {AlarmID}
        // Value: {AlarmStatus, AlarmHour, AlarmMinute, AlarmRingTone, AlarmVibrate, AlarmRepeat, AlarmLabel}
        // All above Value fields delimited by "---"
        try {
            NPDB NPDBInstance = new NPDB(context);
            SQLiteDatabase db = NPDBInstance.getWritableDatabase();
            ContentValues alarmRow = new ContentValues();
            alarmRow.put(COLUMN_ALARM_ID, alarmID);
            alarmRow.put(COLUMN_ALARM_TYPE, alarmType);
            alarmRow.put(COLUMN_ALARM_STATE, isAlarmOn);
            alarmRow.put(COLUMN_ALARM_HOUROFDAY, alarmHourOfDay);
            alarmRow.put(COLUMN_ALARM_MINUTE, alarmMin);
            alarmRow.put(COLUMN_ALARM_RINGTONE, ringTone);
            alarmRow.put(COLUMN_ALARM_VIBRATE, toVibrate);
            alarmRow.put(COLUMN_ALARM_REPEAT, repeatOption);
            alarmRow.put(COLUMN_ALARM_LABEL, label);
            long dbResult = db.insert(NP_ALARM_TABLE, null, alarmRow);
            db.close();

            /*if (dbResult != DB_RESULT_FAILURE) {
                Log.i("NPDB", "Alarm(" + alarmID + ") ADDED to DB!");
            } else {
                Log.i("NPDB", "Alarm(" + alarmID + ") FAILED to ADD to DB!" +
                        "Result Code: " + dbResult);
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            //Log.i("NPDB","ERROR: Alarm(" + alarmID + ") NOT ADDED to DB!");
        }
        //long endTime = System.nanoTime();
        //Log.d("AlarmDB","addAlarmToDB(): Time Taken: " + calcTimeDiff(startTime, endTime));
    }

    /**
     * Use this API to update Alarm information in persistent DB.
     *
     * @param context           App Context
     * @param alarmID           Alarm ID
     * @param isAlarmOn         On/Off
     * @param alarmHourOfDay    Alarm Hour of Day (24-hour format)
     * @param alarmMin          Alarm Minutes
     * @param ringTone          Full path to Ringtone
     * @param toVibrate         True - Vibrate, False - Do NOT Vibrate
     * @param repeatOption      Once   - Buzz Alarm one-time
     *                          Daily  - Buzz Alarm daily at the Alarm Hour & Minute
     *                          Custom - Buzz Alarm at the Alarm Hour & Minute on selected days
     * @param label             Alarm Label
     */
    public static void updateAlarmInfoInDB(Context context, boolean alarmType, int alarmID,
                                           boolean isAlarmOn, int alarmHourOfDay, int alarmMin,
                                           String ringTone, boolean toVibrate, int repeatOption,
                                           String label) {
        //long startTime = System.nanoTime();
        //Log.i("NPDB","Updating Alarm(" + alarmID + ") Info in DB!");

        try {
            NPDB NPDBInstance = new NPDB(context);
            SQLiteDatabase db = NPDBInstance.getWritableDatabase();
            ContentValues alarmRow = new ContentValues();
            alarmRow.put(COLUMN_ALARM_ID, alarmID);
            alarmRow.put(COLUMN_ALARM_TYPE, alarmType);
            alarmRow.put(COLUMN_ALARM_STATE, isAlarmOn);
            alarmRow.put(COLUMN_ALARM_HOUROFDAY, alarmHourOfDay);
            alarmRow.put(COLUMN_ALARM_MINUTE, alarmMin);
            alarmRow.put(COLUMN_ALARM_RINGTONE, ringTone);
            alarmRow.put(COLUMN_ALARM_VIBRATE, toVibrate);
            alarmRow.put(COLUMN_ALARM_REPEAT, repeatOption);
            alarmRow.put(COLUMN_ALARM_LABEL, label);
            long dbResult = db.update(NP_ALARM_TABLE, alarmRow,
                    COLUMN_ALARM_ID + "=" + alarmID, null);
            db.close();

            /*if (dbResult != DB_RESULT_FAILURE) {
                //Log.i("NPDB", "Alarm(" + alarmID + ") UPDATED in DB!");
            } else {
                //Log.i("NPDB", "Alarm(" + alarmID + ") FAILED to UPDATE to DB!" +
                        "Result Code: " + dbResult);
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            //Log.i("NPDB","ERROR: Alarm(" + alarmID + ") NOT UPDATED in DB!");
        }
        //long endTime = System.nanoTime();
        //Log.d("AlarmDB","updateAlarmInfoInDB(): Time Taken: " + calcTimeDiff(startTime, endTime));
    }

    /**
     * Use this API to update Alarm state in persistent DB.
     *
     * @param context     App Context
     * @param alarmID     Alarm ID
     * @param isAlarmOn   On/Off
     */
    public static void updateAlarmStateInDB(Context context, int alarmID, boolean isAlarmOn) {
        //long startTime = System.nanoTime();
        //Log.i("NPDB","Updating Alarm(" + alarmID + ") state to DB!");

        try {
            NPDB NPDBInstance = new NPDB(context);
            SQLiteDatabase db = NPDBInstance.getWritableDatabase();
            ContentValues alarmRow = new ContentValues();
            alarmRow.put(COLUMN_ALARM_ID, alarmID);
            alarmRow.put(COLUMN_ALARM_STATE, isAlarmOn);
            long dbResult = db.update(NP_ALARM_TABLE, alarmRow,
                    COLUMN_ALARM_ID + "=" + alarmID, null);
            db.close();

            /*if (dbResult != DB_RESULT_FAILURE) {
                //Log.i("NPDB", "Alarm(" + alarmID + ") UPDATED in DB!");
            } else {
                //Log.i("NPDB", "Alarm(" + alarmID + ") FAILED to UPDATE to DB!" +
                        "Result Code: " + dbResult);
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            //Log.i("NPDB","ERROR: Alarm(" + alarmID + ") NOT UPDATED in DB!");
        }
        //long endTime = System.nanoTime();
        //Log.d("AlarmDB","updateAlarmStateInDB(): Time Taken: " + calcTimeDiff(startTime, endTime));
    }

    /**
     * Use this API to remove Alarm information from persistent DB.
     *
     * @param context     App Context
     * @param alarmID     Alarm ID
     */
    public static void removeAlarmFromDB(Context context, int alarmID) {
        //long startTime = System.nanoTime();
        //Log.i("NPDB","Removing Alarm(" + alarmID + ") from DB!");

        try {
            NPDB NPDBInstance = new NPDB(context);
            SQLiteDatabase db = NPDBInstance.getWritableDatabase();
            long dbResult = db.delete(NP_ALARM_TABLE, COLUMN_ALARM_ID + "=" + alarmID, null);
            db.close();

            /*if (dbResult != DB_RESULT_FAILURE) {
                Log.i("NPDB", "Alarm(" + alarmID + ") DELETED from DB!");
            } else {
                Log.i("NPDB", "Alarm(" + alarmID + ") FAILED to DELETE from DB!" +
                        "Result Code: " + dbResult);
            }*/
            //Log.i("NPDB","Alarm(" + alarmID + ") REMOVED from DB!");
        } catch (Exception e) {
            e.printStackTrace();
            //Log.i("NPDB","ERROR: Alarm(" + alarmID + ") NOT REMOVED from DB!");
        }
        //long endTime = System.nanoTime();
        //Log.d("AlarmDB","removeAlarmFromDB(): Time Taken: " + calcTimeDiff(startTime, endTime));
    }

    /**
     * Use this API to read Alarm information from persistent DB & return a hasmap of list of Alarms.
     *
     * @param context    App Context
     *
     * @return  Hashmap of List of Alarm Information.
     */
    public static HashMap<Integer, AlarmInfo> readAlarmsFromDB (Context context) {
        //long startTime = System.nanoTime();
        HashMap<Integer, AlarmInfo> alarmsHashList = new HashMap<>();
        int numAlarms = 0;

        try {
            NPDB NPDBInstance = new NPDB(context);
            SQLiteDatabase db = NPDBInstance.getReadableDatabase();
            Cursor cursor = db.rawQuery("select * from NP_ALARM_TABLE", null);
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    int alarmID = cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_ID));
                    boolean alarmType = Alarm.ALARM_TYPE_STANDARD;
                    boolean isAlarmOn = false;
                    if (cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_STATE)) == 1) {
                        isAlarmOn = true;
                    }
                    int alarmHourOfDay = cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_HOUROFDAY));
                    int alarmMin = cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_MINUTE));
                    String ringTone = cursor.getString(cursor.getColumnIndex(COLUMN_ALARM_RINGTONE));
                    boolean toVibrate = false;
                    if (cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_VIBRATE)) == 1) {
                        toVibrate = true;
                    }
                    int repeatOption = cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_REPEAT));
                    String label = cursor.getString(cursor.getColumnIndex(COLUMN_ALARM_LABEL));

                    alarmsHashList.put(alarmID, new AlarmInfo(alarmID, alarmType, isAlarmOn,
                                       alarmHourOfDay, alarmMin, ringTone, toVibrate,
                                       repeatOption, label));
                    numAlarms += 1;
                    cursor.moveToNext();
                }
            }
            cursor.close();
            db.close();
            //Log.i("NPDB", "(" + numAlarms + ") Alarms READ FROM DB!");
        } catch (Exception e) {
            e.printStackTrace();
            //Log.i("NPDB","ERROR: Alarms NOT READ FROM DB!");
        }

        HashMap<Integer, AlarmInfo> alarmsDB = sortAlarms(alarmsHashList);
        if (alarmsDB == null) {
            alarmsDB = alarmsHashList;
        }

        //long endTime = System.nanoTime();
        //Log.d("AlarmDB","readAlarmsFromDB(): Time Taken: " + calcTimeDiff(startTime, endTime));
        return alarmsDB;
    }

    /**
     * Use this API to retrieve list of Alarm IDs from persistent DB.
     *
     * @param alarmsListMap     Hashmap of list of Alarm Information
     *
     * @return Hashmap of List of Alarm IDs.
     */
    public static ArrayList<Integer> getAlarmIDs(HashMap<Integer, AlarmInfo> alarmsListMap){
        ArrayList<Integer> arrayList = new ArrayList<>(alarmsListMap.keySet());
        if (arrayList.size() > 0) {
            return arrayList;
        }
        return null;
    }

    /**
     * Use this API to check if an Alarm is present in persistent DB.
     *
     * @param context           App Context
     * @param alarmID           Alarm ID
     *
     * @return True - if present, False - otherwise.
     */
    public static boolean isAlarmInDB(Context context, int alarmID) {
        //Log.i("NPDB","Checking if alarm(" + alarmID + ") is present in DB!");
        HashMap<Integer, AlarmInfo> alarmsDB = readAlarmsFromDB(context);
        if (alarmsDB.size() > 0) {
            return (alarmsDB.get(alarmID) != null);
        }
        return false;
    }

    /**
     * Use this API to add Alarm information to persistent DB.
     *
     * @param context           App Context
     * @param reminderID        Reminder ID
     * @param isAlarmOn         On/Off
     * @param alarmHourOfDay    Alarm Hour of Day (24-hour format)
     * @param alarmMin          Alarm Minutes
     * @param ringTone          Full path to Ringtone
     * @param toVibrate         True - Vibrate, False - Do NOT Vibrate
     * @param repeatOption      Once   - Buzz Alarm one-time
     *                          Daily  - Buzz Alarm daily at the Alarm Hour & Minute
     *                          Custom - Buzz Alarm at the Alarm Hour & Minute on selected days
     * @param label             Alarm Label
     */
    public static void addReminderToDB(Context context, boolean alarmType, int reminderID,
                                       boolean isAlarmOn, int alarmHourOfDay, int alarmMin,
                                       String ringTone, boolean toVibrate, int repeatOption,
                                       String label, int iconID) {
        //long startTime = System.nanoTime();
        //Log.i("NPDB","Adding Reminder(" + reminderID + ") to DB!");

        // Alarm DB Record:
        // Key: {AlarmID}
        // Value: {AlarmStatus, AlarmHour, AlarmMinute, AlarmRingTone, AlarmVibrate, AlarmRepeat, AlarmLabel}
        // All above Value fields delimited by "---"
        try {
            NPDB NPDBInstance = new NPDB(context);
            SQLiteDatabase db = NPDBInstance.getWritableDatabase();
            ContentValues reminderRow = new ContentValues();
            reminderRow.put(COLUMN_ALARM_ID, reminderID);
            reminderRow.put(COLUMN_ALARM_TYPE, alarmType);
            reminderRow.put(COLUMN_ALARM_STATE, isAlarmOn);
            reminderRow.put(COLUMN_ALARM_HOUROFDAY, alarmHourOfDay);
            reminderRow.put(COLUMN_ALARM_MINUTE, alarmMin);
            reminderRow.put(COLUMN_ALARM_RINGTONE, ringTone);
            reminderRow.put(COLUMN_ALARM_VIBRATE, toVibrate);
            reminderRow.put(COLUMN_ALARM_REPEAT, repeatOption);
            reminderRow.put(COLUMN_ALARM_LABEL, label);
            reminderRow.put(COLUMN_ALARM_ICON_ID, iconID);
            long dbResult = db.insertWithOnConflict(NP_REMINDER_TABLE, null,
                                                    reminderRow, SQLiteDatabase.CONFLICT_IGNORE);
            db.close();

            /*if (dbResult != DB_RESULT_FAILURE) {
                Log.i("NPDB", "Reminder(" + reminderID + ") ADDED to DB!");
            } else {
                Log.i("NPDB", "Reminder(" + reminderID + ") FAILED to ADD to DB!" +
                        "Result Code: " + dbResult);
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            //Log.i("NPDB","ERROR: Reminder(" + reminderID + ") NOT ADDED to DB!");
        }
        //long endTime = System.nanoTime();
        //Log.d("AlarmDB","addReminderToDB(): Time Taken: " + calcTimeDiff(startTime, endTime));
    }

    /**
     * Use this API to update Reminder information in persistent DB.
     *
     * @param context           App Context
     * @param reminderID        Reminder ID
     * @param isAlarmOn         On/Off
     * @param alarmHourOfDay    Alarm Hour of Day (24-hour format)
     * @param alarmMin          Alarm Minutes
     * @param ringTone          Full path to Ringtone
     * @param toVibrate         True - Vibrate, False - Do NOT Vibrate
     * @param repeatOption      Once   - Buzz Alarm one-time
     *                          Daily  - Buzz Alarm daily at the Alarm Hour & Minute
     *                          Custom - Buzz Alarm at the Alarm Hour & Minute on selected days
     * @param label             Alarm Label
     */
    public static void updateReminderInfoInDB(Context context, boolean alarmType, int reminderID,
                                              boolean isAlarmOn, int alarmHourOfDay, int alarmMin,
                                              String ringTone, boolean toVibrate,
                                              int repeatOption, String label, int iconID) {
        //long startTime = System.nanoTime();
        //Log.i("NPDB","Updating Reminder(" + reminderID + ") Info in DB!");

        try {
            NPDB NPDBInstance = new NPDB(context);
            SQLiteDatabase db = NPDBInstance.getWritableDatabase();
            ContentValues reminderRow = new ContentValues();
            reminderRow.put(COLUMN_ALARM_ID, reminderID);
            reminderRow.put(COLUMN_ALARM_TYPE, alarmType);
            reminderRow.put(COLUMN_ALARM_STATE, isAlarmOn);
            reminderRow.put(COLUMN_ALARM_HOUROFDAY, alarmHourOfDay);
            reminderRow.put(COLUMN_ALARM_MINUTE, alarmMin);
            reminderRow.put(COLUMN_ALARM_RINGTONE, ringTone);
            reminderRow.put(COLUMN_ALARM_VIBRATE, toVibrate);
            reminderRow.put(COLUMN_ALARM_REPEAT, repeatOption);
            reminderRow.put(COLUMN_ALARM_LABEL, label);
            reminderRow.put(COLUMN_ALARM_ICON_ID, iconID);
            long dbResult = db.update(NP_REMINDER_TABLE, reminderRow,
                    COLUMN_ALARM_ID + "=" + reminderID, null);
            db.close();

            /*if (dbResult != DB_RESULT_FAILURE) {
                //Log.i("NPDB","Reminder(" + reminderID + ") UPDATED in DB!");
            } else {
                //Log.i("NPDB", "Reminder(" + reminderID + ") FAILED to UPDATE to DB!" +
                        "Result Code: " + dbResult);
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            //Log.i("NPDB","ERROR: Reminder(" + reminderID + ") NOT UPDATED in DB!");
        }
        //long endTime = System.nanoTime();
        //Log.d("AlarmDB","updateReminderInfoInDB(): Time Taken: " + calcTimeDiff(startTime, endTime));
    }

    /**
     * Use this API to update Reminder state in persistent DB.
     *
     * @param context       App Context
     * @param reminderID    Reminder ID
     * @param isAlarmOn     On/Off
     */
    public static void updateReminderStateInDB(Context context, int reminderID, boolean isAlarmOn) {
        //long startTime = System.nanoTime();
        //Log.i("NPDB","Updating Reminder(" + reminderID + ") state to DB!");

        try {
            NPDB NPDBInstance = new NPDB(context);
            SQLiteDatabase db = NPDBInstance.getWritableDatabase();
            ContentValues reminderRow = new ContentValues();
            reminderRow.put(COLUMN_ALARM_ID, reminderID);
            reminderRow.put(COLUMN_ALARM_STATE, isAlarmOn);
            long dbResult = db.update(NP_REMINDER_TABLE, reminderRow,
                    COLUMN_ALARM_ID + "=" + reminderID, null);
            db.close();
            /*if (dbResult != DB_RESULT_FAILURE) {
                //Log.i("NPDB","Reminder(" + reminderID + ") UPDATED in DB!");
            } else {
                //Log.i("NPDB", "Reminder(" + reminderID + ") FAILED to UPDATE to DB!" +
                        "Result Code: " + dbResult);
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            //Log.i("NPDB","ERROR: Reminder(" + reminderID + ") NOT UPDATED in DB!");
        }
        //long endTime = System.nanoTime();
        //Log.d("AlarmDB","updateReminderStateInDB(): Time Taken: " + calcTimeDiff(startTime, endTime));
    }

    /**
     * Use this API to remove Reminder information from persistent DB.
     *
     * @param context       App Context
     * @param reminderID    Alarm ID
     */
    public static void removeReminderFromDB(Context context, int reminderID) {
        //long startTime = System.nanoTime();
        //Log.i("NPDB","Removing Reminder(" + reminderID + ") from DB!");

        try {
            NPDB NPDBInstance = new NPDB(context);
            SQLiteDatabase db = NPDBInstance.getWritableDatabase();
            long dbResult = db.delete(NP_REMINDER_TABLE,
                    COLUMN_ALARM_ID + "=" + reminderID, null);
            db.close();
            /*if (dbResult != DB_RESULT_FAILURE) {
                //Log.i("NPDB","Reminder(" + reminderID + ") DELETED from DB!");
            } else {
                //Log.i("NPDB", "Reminder(" + reminderID + ") FAILED to DELETE from DB!" +
                        "Result Code: " + dbResult);
            }*/
            //Log.i("NPDB","Reminder(" + reminderID + ") REMOVED from DB!");
        } catch (Exception e) {
            e.printStackTrace();
            //Log.i("NPDB","ERROR: Reminder(" + reminderID + ") NOT REMOVED from DB!");
        }
        //long endTime = System.nanoTime();
        //Log.d("AlarmDB","removeReminderFromDB(): Time Taken: " + calcTimeDiff(startTime, endTime));
    }

    /**
     * Use this API to read Reminder information from persistent DB & return a list of Reminders.
     *
     * @param context           App Context
     *
     * @return  Hashmap of List of Alarm Information.
     */
    public static HashMap<Integer, ReminderInfo> readRemindersFromDB(Context context) {
        //long startTime = System.nanoTime();
        HashMap<Integer, ReminderInfo> remindersHashList = new HashMap<>();
        int numReminders = 0;

        try {
            NPDB NPDBInstance = new NPDB(context);
            SQLiteDatabase db = NPDBInstance.getReadableDatabase();
            Cursor cursor = db.rawQuery("select * from NP_REMINDER_TABLE", null);
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    int alarmID = cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_ID));
                    boolean alarmType = Alarm.ALARM_ALARM_TYPE_VEDIC;
                    boolean isAlarmOn = false;
                    if (cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_STATE)) == 1) {
                        isAlarmOn = true;
                    }
                    int alarmHourOfDay = cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_HOUROFDAY));
                    int alarmMin = cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_MINUTE));
                    String ringTone = cursor.getString(cursor.getColumnIndex(COLUMN_ALARM_RINGTONE));
                    boolean toVibrate = false;
                    if (cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_VIBRATE)) == 1) {
                        toVibrate = true;
                    }
                    int repeatOption = cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_REPEAT));
                    String label = cursor.getString(cursor.getColumnIndex(COLUMN_ALARM_LABEL));
                    int iconID = cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_ICON_ID));

                    remindersHashList.put(alarmID, new ReminderInfo(alarmID, alarmType, isAlarmOn,
                                          alarmHourOfDay, alarmMin, ringTone, toVibrate,
                                          repeatOption, label, iconID));
                    numReminders += 1;
                    cursor.moveToNext();
                }
            }
            cursor.close();
            db.close();
            //Log.i("NPDB", "(" + numReminders + ") Reminders READ FROM DB!");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
            //Log.i("NPDB","ERROR: Reminders NOT READ FROM DB!");
        }

        HashMap<Integer, ReminderInfo> remindersDB = sortReminders(remindersHashList);
        if (remindersDB == null) {
            remindersDB = remindersHashList;
        }

        //long endTime = System.nanoTime();
        //Log.d("AlarmDB","readRemindersFromDB(): Time Taken: " + calcTimeDiff(startTime, endTime));
        return remindersDB;
    }

    /**
     * Use this API to retrieve list of Reminder IDs from persistent DB.
     *
     * @param remindersListMap     Hashmap of list of Reminder Information
     *
     * @return Hashmap of List of Alarm IDs.
     */
    public static ArrayList<Integer> getReminderIDs(HashMap<Integer, ReminderInfo> remindersListMap){
        ArrayList<Integer> arrayList = new ArrayList<>(remindersListMap.keySet());
        if (arrayList.size() > 0) {
            return arrayList;
        }
        return null;
    }

    /**
     * Use this utility function to compare 2 alarms/reminders and
     * check if Time{Hour, Min} is higher, lower or same.
     *
     * @param leftValue     Left value to compare
     * @param rightValue    Right value to compare
     *
     * @return  1 if lefValue's AlarmTime is higher than that of rightValue.
     *         -1 if lefValue's AlarmTime is lower than that of rightValue.
     *          0 if lefValue's AlarmTime is the same as that of rightValue.
     */
    private static int compareAlarmTimes (AlarmInfo leftValue, AlarmInfo rightValue) {
        int leftHourOfDay;
        int rightHourOfDay;
        int leftMin;
        int rightMin;

        leftHourOfDay = leftValue.alarmHourOfDay;
        leftMin = leftValue.alarmMin;
        rightHourOfDay = rightValue.alarmHourOfDay;
        rightMin = rightValue.alarmMin;

        // For ascending order sort:
        // 1) If left hour is greater than right hour
        // 2) If left & right hours are the same, but left minutes is greater than right
        if (leftHourOfDay > rightHourOfDay) {
            return 1;
        } else if (leftHourOfDay == rightHourOfDay) {
            return Integer.compare(leftMin, rightMin);
        }

        return -1;
    }

    /**
     * Use this utility function to sort Alarm information in ascending order.
     *
     * @param listMap Hashmap of list of Alarm/Reminder Information
     *
     * @return Return a list of alarms sorted in ascending order (or) null upon failure.
     */
    private static LinkedHashMap<Integer, AlarmInfo> sortAlarms(HashMap<Integer, AlarmInfo> listMap) {
        Set<Map.Entry<Integer, AlarmInfo>> set = listMap.entrySet();
        List<Map.Entry<Integer, AlarmInfo>> list = new ArrayList<>(set);
        Collections.sort(list, (o1, o2) -> compareAlarmTimes(o1.getValue(), o2.getValue()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return list.stream().collect(
                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b,
                            LinkedHashMap::new));
        }
        return null;
    }

    /**
     * Use this utility function to sort Alarm information in ascending order.
     *
     * @param alarmsListMap Hashmap of list of Alarm Information
     *
     * @return Return a list of alarms sorted in ascending order (or) null upon failure.
     */
    private static LinkedHashMap<Integer, ReminderInfo> sortReminders(HashMap<Integer, ReminderInfo> alarmsListMap) {
        Set<Map.Entry<Integer, ReminderInfo>> set = alarmsListMap.entrySet();
        List<Map.Entry<Integer, ReminderInfo>> list = new ArrayList<>(set);
        Collections.sort(list, (o1, o2) -> compareAlarmTimes(o1.getValue(), o2.getValue()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return list.stream().collect(
                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b,
                            LinkedHashMap::new));
        }
        return null;
    }

    /*public static String calcTimeDiff(long startTime, long endTime) {
        long duration = (endTime - startTime);
        long us = duration / 1000;
        long ms = us / 1000;
        long sec = ms / 1000;
        if (us > 1000) {
            us %= 1000;
            ms += 1;
        }
        if (ms > 1000) {
            ms %= 1000;
            sec += 1;
        }

        return String.format("(%02ds:%02dms:%02dus)", sec, ms, us);
    }*/
}