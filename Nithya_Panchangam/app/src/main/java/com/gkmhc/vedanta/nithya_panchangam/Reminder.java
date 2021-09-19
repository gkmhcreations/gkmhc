package com.gkmhc.vedanta.nithya_panchangam;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.gkmhc.utils.VedicCalendar;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Reminder fragment to handle list of Reminders.
 * Handle {Create, Delete, Schedule, Stop} Reminder functions.
 * Reuse most of Alarm behavior but focus more on "Panchangam" based Alarms.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class Reminder extends Fragment {
    private Context context;
    private ListView reminderListView;
    public static final int DEFAULT_REMINDER_HOUR_OF_DAY = 6;
    public static final int DEFAULT_REMINDER_MIN = 0;
    public static final int REMINDER_REQUEST_CODE = 3457;

    public Reminder() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getContext();
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.updateAppLocale();
        }

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_reminder, container, false);
        reminderListView = root.findViewById(R.id.reminder_table);

        refreshReminders();
        return root;
    }

    public void refreshReminders() {
        new Thread() {
            @Override
            public void run() {
                try {
                    // code runs in a thread
                    requireActivity().runOnUiThread(() -> updateRemindersListView(-1));
                } catch (final Exception ex) {
                    Log.d("Reminder","Exception in initReminders()");
                }
            }
        }.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Log.i("Reminder","Request Code: " + requestCode + " Response Code: " + resultCode);

        // Upon Activity Results, do the following:
        // 1) Create new Reminder based on information provided (not yet supported)
        // 2) Update Reminder with the information provided by the user
        if (requestCode == REMINDER_REQUEST_CODE) {
            if (data != null) {
                int reminderID = data.getIntExtra(Alarm.EXTRA_ALARM_ALARM_ID, -1);
                int alarmHourOfDay = data.getIntExtra(Alarm.EXTRA_ALARM_ALARM_HOUR_OF_DAY, 0);
                int alarmMin = data.getIntExtra(Alarm.EXTRA_ALARM_ALARM_MIN, 0);
                boolean alarmType = data.getBooleanExtra(Alarm.EXTRA_ALARM_TYPE, false);
                boolean toVibrate = data.getBooleanExtra(Alarm.EXTRA_ALARM_VIBRATE, false);
                int repeatOption = data.getIntExtra(Alarm.EXTRA_ALARM_REPEAT, Alarm.ALARM_REPEAT_ONCE);
                String ringTone = data.getStringExtra(Alarm.EXTRA_ALARM_RINGTONE);
                String label = data.getStringExtra(Alarm.EXTRA_ALARM_LABEL);
                int iconID = data.getIntExtra(Alarm.EXTRA_ALARM_ICON_ID, Alarm.DEF_ICON_ID);
                Log.i("Reminder", "Reminder Code: " + " Reminder Time: " + alarmHourOfDay + ":" + alarmMin);

                if (alarmType == Alarm.ALARM_TYPE_VEDIC) {
                    // Scenario: Modify existing alarm
                    if (reminderID != Alarm.INVALID_VALUE) {
                        NPDB.updateAlarmInfoInDB(context, Alarm.ALARM_TYPE_VEDIC,
                                reminderID, Alarm.ALARM_STATE_ON, alarmHourOfDay, alarmMin,
                                ringTone, toVibrate, repeatOption, label);
                        Alarm.restartAlarm(context, alarmType, reminderID, alarmHourOfDay,
                                           alarmMin, ringTone, toVibrate, repeatOption, label,
                                           iconID);
                    } else {
                        Log.e("Reminder", "ERROR: Cannot create new Reminder!");
                    }
                    updateRemindersListView(reminderID);
                }
            }
        }
    }

    private void updateRemindersListView(int scrollToReminder) {
        boolean addToDB = true;
        long pStartTime = System.nanoTime();
        int labelID;

        HashMap<Integer, NPDB.AlarmInfo> remindersDB = NPDB.readAlarmsFromDB(
                context.getApplicationContext(), Alarm.ALARM_TYPE_VEDIC);

        // If there are ZERO reminders in DB, then create DB first-time ONLY! Skip rest of the times.
        if (remindersDB.size() >= VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RANGE_END) {
            addToDB = false;
        }
        ArrayList<Integer> reminderAlarmIDList = new ArrayList<>();
        for (int festivalEventCode = VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RANGE_START;
             festivalEventCode < VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RANGE_END;
             festivalEventCode += 1) {
            reminderAlarmIDList.add(festivalEventCode);
            labelID = getDinaVisheshamLabel(festivalEventCode);

            if (addToDB) {
                NPDB.addAlarmToDB(context, Alarm.ALARM_TYPE_VEDIC, festivalEventCode,
                        Alarm.ALARM_STATE_OFF, DEFAULT_REMINDER_HOUR_OF_DAY, DEFAULT_REMINDER_MIN,
                        "Default", false, Alarm.ALARM_REPEAT_EVERY_OCCURRENCE,
                        getString(labelID));
            }
        }
        long pEndTime = System.nanoTime();
        Log.d("Reminder:", "Overall Time Taken: " +
                VedicCalendar.getTimeTaken(pStartTime, pEndTime));

        if (addToDB) {
            remindersDB = NPDB.readAlarmsFromDB(context.getApplicationContext(),
                                                Alarm.ALARM_TYPE_VEDIC);
        }
        ReminderAdapter reminderAdapter = new ReminderAdapter(context, reminderAlarmIDList,
                remindersDB, this);
        reminderListView.setAdapter(reminderAdapter);

        // -1 indicates - Don't scroll!
        // Basically, after refresh scroll to the reminder that was just updated!
        if (scrollToReminder != -1) {
            try {
                int scrollPos = reminderAlarmIDList.get(scrollToReminder);
                reminderListView.setSelection(scrollPos);
            } catch (Exception e) {
                // Nothing to do!
            }
        }
    }

    /**
     * Utility function to get the user-readable string given a festival code.
     *
     * @return User-readable string in the selected language.
     */
    public static int getDinaVisheshamLabel (int festivalEventCode) {
        int dhinaVishesham;
        switch (festivalEventCode) {
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AMAVAASAI:
                dhinaVishesham = R.string.ammavasai;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_POURNAMI:
                dhinaVishesham = R.string.pournami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI:
                dhinaVishesham = R.string.chithra_pournami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SANKATA_HARA_CHATHURTHI:
                dhinaVishesham = R.string.sankata_hara_chathurthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SASHTI:
                dhinaVishesham = R.string.sashti;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_EKADASI:
                dhinaVishesham = R.string.ekadasi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PRADOSHAM:
                dhinaVishesham = R.string.pradosham;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI:
                dhinaVishesham = R.string.makara_sankaranthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM:
                dhinaVishesham = R.string.thai_poosam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI:
                dhinaVishesham = R.string.vasantha_panchami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI:
                dhinaVishesham = R.string.ratha_saptami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BHISHMA_ASHTAMI:
                dhinaVishesham = R.string.bheeshmashtami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM:
                dhinaVishesham = R.string.maasi_magam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI:
                dhinaVishesham = R.string.bala_periyava_jayanthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI:
                dhinaVishesham = R.string.maha_sivarathiri;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU:
                dhinaVishesham = R.string.karadaiyan_nombu;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRINGERI_PERIYAVA_JAYANTHI:
                dhinaVishesham = R.string.sringeri_periyava_jayanthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM:
                dhinaVishesham = R.string.panguni_uthiram;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_UGADI:
                dhinaVishesham = R.string.ugadi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU:
                dhinaVishesham = R.string.tamil_puthandu;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN:
                dhinaVishesham = R.string.agni_nakshathiram_begin;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END:
                dhinaVishesham = R.string.agni_nakshathiram_end;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI:
                dhinaVishesham = R.string.sri_ramanuja_jayanthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI:
                dhinaVishesham = R.string.sri_rama_navami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI:
                dhinaVishesham = R.string.akshaya_thrithiyai;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI:
                dhinaVishesham = R.string.sankara_jayanthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM:
                dhinaVishesham = R.string.vaikasi_visakam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI:
                dhinaVishesham = R.string.maha_periyava_jayanthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI:
                dhinaVishesham = R.string.puthu_periyava_jayanthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU:
                dhinaVishesham = R.string.aadi_perukku;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM:
                dhinaVishesham = R.string.aadi_pooram;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI:
                dhinaVishesham = R.string.garuda_panchami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM:
                dhinaVishesham = R.string.varalakshmi_vratham;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR:
                dhinaVishesham = R.string.avani_avittam_yajur;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG:
                dhinaVishesham = R.string.avani_avittam_rig;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ONAM:
                dhinaVishesham = R.string.onam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SANKATA_HARA_CHATHURTHI:
                dhinaVishesham = R.string.maha_sankata_hara_chaturthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI:
                dhinaVishesham = R.string.gokulashtami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM:
                dhinaVishesham = R.string.avani_avittam_sam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI:
                dhinaVishesham = R.string.vinayagar_chaturthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI:
                dhinaVishesham = R.string.appayya_dikshitar_jayanthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI:
                dhinaVishesham = R.string.maha_bharani;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI:
                dhinaVishesham = R.string.mahalaya_ammavasai;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START:
                dhinaVishesham = R.string.mahalayam_start;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI:
                dhinaVishesham = R.string.navarathiri;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI:
                dhinaVishesham = R.string.saraswathi_pooja;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI:
                dhinaVishesham = R.string.vijaya_dasami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI:
                dhinaVishesham = R.string.naraka_chathurdasi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI:
                dhinaVishesham = R.string.deepavali;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM:
                dhinaVishesham = R.string.soora_samharam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM:
                dhinaVishesham = R.string.karthigai_deepam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SASHTI_VRATHAM:
                dhinaVishesham = R.string.sashti_vratham;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN:
                dhinaVishesham = R.string.arudhra_darshan;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI:
                dhinaVishesham = R.string.hanuman_jayanthi;
                break;
            default:
                dhinaVishesham = R.string.splash_screen_banner;
                break;
        }

        return dhinaVishesham;
    }

    /**
     * Utility function to get the user-readable string given a festival code.
     *
     * @return User-readable string in the selected language.
     */
    public static int getDinaVisheshamImg(int festivalEventCode) {
        int dhinaVisheshamCode;
        switch (festivalEventCode) {
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AMAVAASAI:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_AMMAVASAI:
                dhinaVisheshamCode = R.drawable.amavaasai;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_POURNAMI:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_CHITHRA_POURNAMI:
                dhinaVisheshamCode = R.drawable.pournami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SANKATA_HARA_CHATHURTHI:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SANKATA_HARA_CHATHURTHI:
                dhinaVisheshamCode = R.drawable.sankata_hara_chathurthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SASHTI:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_PERUKKU:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VAIKASI_VISHAKAM:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SOORA_SAMHAARAM:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SASHTI_VRATHAM:
                dhinaVisheshamCode = R.drawable.sashti;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PANGUNI_UTHIRAM:
                dhinaVisheshamCode = R.drawable.panguni_uthiram;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_EKADASI:
                dhinaVisheshamCode = R.drawable.ekadashi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PRADOSHAM:
                dhinaVisheshamCode = R.drawable.pradhosha_lingam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAKARA_SANKARANTHI:
                dhinaVisheshamCode = R.drawable.makara_sankaranthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_THAI_POOSAM:
                dhinaVisheshamCode = R.drawable.thai_poosam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VASANTHA_PANCHAMI:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SARASWATHI_POOJAI:
                dhinaVisheshamCode = R.drawable.vasantha_panchami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RATHA_SAPTHAMI:
                dhinaVisheshamCode = R.drawable.ratha_sapthami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BHISHMA_ASHTAMI:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_BHARANI:
                dhinaVisheshamCode = R.drawable.ic_generic_event;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHALAYA_START:
                dhinaVisheshamCode = R.drawable.mahalayam_start;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAASI_MAGAM:
                dhinaVisheshamCode = R.drawable.maasi_magam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_BALA_PERIYAVA_JAYANTHI:
                dhinaVisheshamCode = R.drawable.bala_periyava;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_SIVARATHIRI:
                dhinaVisheshamCode = R.drawable.maha_sivarathiri;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARADAIYAN_NOMBHU:
                dhinaVisheshamCode = R.drawable.karadaiyan_nombu;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRINGERI_PERIYAVA_JAYANTHI:
                dhinaVisheshamCode = R.drawable.sringeri_periyava;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_UGADI:
                dhinaVisheshamCode = R.drawable.ugadi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_TAMIL_PUTHANDU:
                dhinaVisheshamCode = R.drawable.tamil_puthandu;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_BEGIN:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AGNI_NAKSHATHRAM_END:
                dhinaVisheshamCode = R.drawable.agni_nakshathram;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_RAMANUJA_JAYANTHI:
                dhinaVisheshamCode = R.drawable.sri_ramanujacharya;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_SRI_RAMA_NAVAMI:
                dhinaVisheshamCode = R.drawable.sri_ramanavami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AKSHAYA_THRITHIYAI:
                dhinaVisheshamCode = R.drawable.maha_lakshmi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ADI_SANKARA_JAYANTHI:
                dhinaVisheshamCode = R.drawable.adi_sankara;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_MAHA_PERIYAVA_JAYANTHI:
                dhinaVisheshamCode = R.drawable.maha_periyava;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_PUTHU_PERIYAVA_JAYANTHI:
                dhinaVisheshamCode = R.drawable.pudhu_periyava;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AADI_POORAM:
                dhinaVisheshamCode = R.drawable.andal;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GARUDA_PANCHAMI:
                dhinaVisheshamCode = R.drawable.garuda_panchami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VARALAKSHMI_VRATHAM:
                dhinaVisheshamCode = R.drawable.varalakshmi_vratham;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_YAJUR:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_RIG:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_AVANI_AVITTAM_SAM:
                dhinaVisheshamCode = R.drawable.avani_avittam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ONAM:
                dhinaVisheshamCode = R.drawable.onam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_GOKULASHTAMI:
                dhinaVisheshamCode = R.drawable.bala_krishnan;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VINAYAGAR_CHATHURTHI:
                dhinaVisheshamCode = R.drawable.ganesh_chathurthi;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_APPAIYA_DIKSHITAR_JAYANTHI:
                dhinaVisheshamCode = R.drawable.appayya_dikshitar;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NAVARATHRI:
                dhinaVisheshamCode = R.drawable.navarathiri;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_VIJAYA_DASHAMI:
                dhinaVisheshamCode = R.drawable.vijaya_dashami;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_NARAKA_CHATHURDASI:
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_DEEPAVALI:
                dhinaVisheshamCode = R.drawable.deepavali;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_KARTHIGAI_DEEPAM:
                dhinaVisheshamCode = R.drawable.karthikai_deepam;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_ARUDHRA_DARSHAN:
                dhinaVisheshamCode = R.drawable.rudra;
                break;
            case VedicCalendar.PANCHANGAM_DHINA_VISHESHAM_HANUMATH_JAYANTHI:
                dhinaVisheshamCode = R.drawable.hanuma_jayanthi;
                break;
            default:
                dhinaVisheshamCode = R.drawable.swamy_ayyappan_circle;
                break;
        }

        return dhinaVisheshamCode;
    }
}