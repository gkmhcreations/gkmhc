package com.gkmhc.vedanta.nithya_panchangam;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Activity for providing an interface to gather Alarm / Reminder details.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class HandleAlarmReminderActivity extends AppCompatActivity {
    private static final int INVALID_VALUE = -1;
    private Menu menu;
    private int alarmID = INVALID_VALUE;
    private int alarmHourOfDay = INVALID_VALUE;
    private int alarmMin = 0;
    private Ringtone ringTonePlayer;
    private String ringTone = "Default";
    private int repeatInput = Alarm.ALARM_REPEAT_ONCE;
    private int repeatOptions = Alarm.ALARM_REPEAT_ONCE;
    private String label = "";
    private int iconID = R.drawable.swamy_ayyappan_circle;
    private int daysSelected = 0;
    private boolean isAlarmUpdate = false;
    private boolean alarmType = Alarm.ALARM_ALARM_TYPE_STANDARD;
    private static final int RINGTONE_ACTIVITY_CODE = 2021;
    private static final int TOTAL_DAYS_IN_WEEK = 7;

    // This activity can be launched for 3 reasons:
    // 1) Add a new Alarm
    // 2) Modify an existing Alarm
    // 3) Add a new reminder (This is not yet supported!)
    // 4) Modify an existing reminder

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainActivity.updateSelLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_alarm);

        boolean toVibrate = false;
        Intent recvdIntent = this.getIntent();

        if (recvdIntent.hasExtra(Alarm.EXTRA_ALARM_TYPE)) {
            alarmType = recvdIntent.getBooleanExtra(Alarm.EXTRA_ALARM_TYPE,
                                                    Alarm.ALARM_ALARM_TYPE_STANDARD);
        }

        // If Alarm Hour is valid then it means an existing alarm/reminder is being modified.
        // If not, then this activity is launched to add a new alarm/reminder.
        if (recvdIntent.hasExtra(Alarm.EXTRA_ALARM_ALARM_HOUR_OF_DAY)) {
            isAlarmUpdate = true;
            alarmID = recvdIntent.getIntExtra(Alarm.EXTRA_ALARM_ALARM_ID, Alarm.INVALID_VALUE);
            alarmHourOfDay = recvdIntent.getIntExtra(Alarm.EXTRA_ALARM_ALARM_HOUR_OF_DAY, INVALID_VALUE);
            alarmMin = recvdIntent.getIntExtra(Alarm.EXTRA_ALARM_ALARM_MIN, INVALID_VALUE);
            repeatInput = recvdIntent.getIntExtra(Alarm.EXTRA_ALARM_REPEAT, Alarm.INVALID_VALUE);
            repeatOptions = repeatInput;
            ringTone = recvdIntent.getStringExtra(Alarm.EXTRA_ALARM_RINGTONE);
            toVibrate = recvdIntent.getBooleanExtra(Alarm.EXTRA_ALARM_VIBRATE, false);
            label = recvdIntent.getStringExtra(Alarm.EXTRA_ALARM_LABEL);
            iconID = recvdIntent.getIntExtra(Alarm.EXTRA_ALARM_ICON_ID, Alarm.DEF_ICON_ID);
        }
        TextView alarmTimeVal = findViewById(R.id.alarmTimeValue);
        TextView repeatDetails = findViewById(R.id.repeat_details);
        repeatDetails.setText(R.string.repeat_once);
        SwitchCompat vibrateSettings = findViewById(R.id.vibration_settings);
        vibrateSettings.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateVisbilityForMenuItem(true));

        EditText labelText = findViewById(R.id.label_text);
        labelText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateVisbilityForMenuItem(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Use this to launch a Android page to allow user to select a ringtone of his/her choice.
        ImageView selectRingTone = findViewById(R.id.ringtone_settings);
        selectRingTone.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Ringtone");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
            startActivityForResult(intent, RINGTONE_ACTIVITY_CODE);
        });

        TimePicker timePicker = findViewById(R.id.alarmSetTime);
        timePicker.setIs24HourView(true);

        // If this activity is launched to modify alarm or reminder the show the UI with
        // Alarm / Reminder details.
        if (isAlarmUpdate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timePicker.setHour(alarmHourOfDay);
                timePicker.setMinute(alarmMin);
            }
            alarmTimeVal.setText(getString(R.string.alarm_time) +
                    String.format("%02d:%02d", alarmHourOfDay, alarmMin));
            if (repeatInput == Alarm.ALARM_REPEAT_ONCE) {
                repeatDetails.setText(R.string.repeat_once);
            } else if (repeatInput == Alarm.ALARM_REPEAT_DAILY) {
                repeatDetails.setText(R.string.repeat_daily);
            } else if (repeatInput == Alarm.ALARM_REPEAT_EVERY_OCCURRENCE) {
                repeatDetails.setText(R.string.repeat_every_occurrence);
            } else {
                repeatDetails.setText(R.string.repeat_custom);
            }
            vibrateSettings.setChecked(toVibrate);
            labelText.setText(label);
        }

        timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            alarmHourOfDay = hourOfDay;
            alarmMin = minute;
            updateVisbilityForMenuItem(true);
            alarmTimeVal.setText(getString(R.string.alarm_time) +
                    String.format("%02d:%02d", hourOfDay, minute));
        });

        RelativeLayout layout = findViewById(R.id.repeatLayout);
        layout.setOnClickListener(v -> showRepeatOptionSelectDialog());

        if (alarmType == Alarm.ALARM_TYPE_PANCHANGAM) {
            labelText.setEnabled(false);
            TextView textView = findViewById(R.id.alarmTypeText);
            textView.setText(R.string.vedic);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result of the activity after user ringtone selection or cancellation
        if ((resultCode == Activity.RESULT_OK) && (requestCode == RINGTONE_ACTIVITY_CODE)) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                ringTone = uri.toString();
                if (ringTonePlayer != null) {
                    ringTonePlayer.stop();
                }

                try {
                    ringTonePlayer = RingtoneManager.getRingtone(getApplicationContext(), uri);
                    ringTonePlayer.setStreamType(AudioManager.STREAM_ALARM);
                    ringTonePlayer.play();
                    Log.i("AlarmReminderActivity", "Playing Ringtone: " + ringTone);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("AlarmReminderActivity", "Unable to play Ringtone: " + ringTone);
                }
            } else {
                // In case no ringtone was selected, then default to "Default" Ringtone
                ringTone = "Default";
            }
            updateVisbilityForMenuItem(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop playing Ringtone when Activity is closed/back button is pressed.
        if (ringTonePlayer != null) {
            ringTonePlayer.stop();
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
            setAlarm();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Show menu item to "click" OK only when some Alarm details are provided.
    // Modification of any/all of the following can result in this function being called:
    // 1) Alarm time is set / modified
    // 2) Ringtone is selected
    // 3) Vibrator is selected
    // 4) Label is typed
    private void updateVisbilityForMenuItem (boolean visibility) {
        if (menu != null) {
            menu.getItem(0).setVisible(visibility);
        }
    }

    // This utility function is called to gather all Alarm information and share the details
    // with calling Activity for Alarm (or) Reminder creation/modification processing.
    private void setAlarm () {
        Log.i("AddAlarm:", "Creating Alarm...");
        Intent retIntent = new Intent();
        if (isAlarmUpdate) {
            retIntent.putExtra(Alarm.EXTRA_ALARM_ALARM_ID, alarmID);
        }
        Calendar calendar = Calendar.getInstance();
        if ((alarmHourOfDay == INVALID_VALUE) || (alarmMin == INVALID_VALUE)) {
            alarmHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
            alarmMin = calendar.get(Calendar.MINUTE);
        }

        // Prepare an intent with following information
        // 1) Alarm Type (Standard / Panchangam
        // 2) Hour of Day
        // 3) Minute
        // 4) Ringtone
        // 5) Vibration
        // 6) Repeat Options
        //    Alarm - Repeat: Once, Daily, Custom (Sun to Sat)
        //    Reminder - Repeat: Once, Every Occurrence
        // 7) Label - User-defined label text (empty if no input)
        // 8) Icon - TODO: Give an option to user, in future versions, to allow to choose an ICON
        retIntent.putExtra(Alarm.EXTRA_ALARM_TYPE, alarmType);
        retIntent.putExtra(Alarm.EXTRA_ALARM_ALARM_HOUR_OF_DAY, alarmHourOfDay);
        retIntent.putExtra(Alarm.EXTRA_ALARM_ALARM_MIN, alarmMin);
        retIntent.putExtra(Alarm.EXTRA_ALARM_RINGTONE, ringTone);
        SwitchCompat switchCompat = findViewById(R.id.vibration_settings);
        boolean setingsVal = switchCompat.isChecked();
        retIntent.putExtra(Alarm.EXTRA_ALARM_VIBRATE, setingsVal);
        retIntent.putExtra(Alarm.EXTRA_ALARM_REPEAT, repeatOptions);
        EditText labelText = findViewById(R.id.label_text);
        label = labelText.getText().toString();
        retIntent.putExtra(Alarm.EXTRA_ALARM_LABEL, label);
        retIntent.putExtra(Alarm.EXTRA_ALARM_ICON_ID, iconID);
        setResult(RESULT_OK, retIntent);
        finish();
    }

    // This utility function is used to show repeat options for user selection
    // Alarm -
    // Repeat - Alarm
    //             a) Once - Option to fire Alarm once based on Calendar date/time
    //             a) Daily - Option to fire Alarm daily
    //             a) Custom - Option to fire Alarm as per day selection (Sun,Mon,Tue,Wed,Thu,Fri,Sat}
    //          Reminder
    //             a) Once - Option to fire Alarm once "Dhina Vishesham"
    //             b) Every Occurrence - Option to fire Alarm when "Dhina Vishesham" matches
    //                the selected Alarm
    private void showRepeatOptionSelectDialog () {
        TextView repeatOptionsText = findViewById(R.id.repeat_details);
        LayoutInflater inflater = LayoutInflater.from(this);
        View repeatOptionsSelectView =
                inflater.inflate(R.layout.repeat_option_dialog, null);

        CheckBox repeatOnceCheckBox = repeatOptionsSelectView.findViewById(R.id.repeat_once);
        CheckBox repeatDailyCheckBox = repeatOptionsSelectView.findViewById(R.id.repeat_daily);
        CheckBox repeatCustomCheckBox = repeatOptionsSelectView.findViewById(R.id.repeat_custom);
        CheckBox repeatEveryOccurCheckBox = repeatOptionsSelectView.findViewById(R.id.repeat_every_occurrence);
        RelativeLayout repeatInputLayout = repeatOptionsSelectView.findViewById(R.id.repeat_custom_suboptions);

        if (alarmType == Alarm.ALARM_TYPE_PANCHANGAM) {
            repeatEveryOccurCheckBox.setVisibility(View.VISIBLE);
            repeatDailyCheckBox.setVisibility(View.GONE);
            repeatCustomCheckBox.setVisibility(View.GONE);
            repeatInputLayout.setVisibility(View.GONE);
        } else {
            repeatEveryOccurCheckBox.setVisibility(View.GONE);
            repeatDailyCheckBox.setVisibility(View.VISIBLE);
            repeatCustomCheckBox.setVisibility(View.VISIBLE);
        }

        repeatOnceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                repeatEveryOccurCheckBox.setChecked(false);
                repeatDailyCheckBox.setChecked(false);
                repeatCustomCheckBox.setChecked(false);
                repeatInputLayout.setVisibility(View.GONE);
            }
        });
        repeatDailyCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                repeatOnceCheckBox.setChecked(false);
                repeatCustomCheckBox.setChecked(false);
                repeatInputLayout.setVisibility(View.GONE);
            }
        });
        repeatCustomCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                repeatDailyCheckBox.setChecked(false);
                repeatOnceCheckBox.setChecked(false);
                repeatInputLayout.setVisibility(View.VISIBLE);
            }
        });
        repeatEveryOccurCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                repeatOnceCheckBox.setChecked(false);
            }
        });
        CheckBox repeatCustomSunCheckBox =
                repeatOptionsSelectView.findViewById(R.id.repeat_custom_sunday);
        CheckBox repeatCustomMonCheckBox =
                repeatOptionsSelectView.findViewById(R.id.repeat_custom_monday);
        CheckBox repeatCustomTueCheckBox =
                repeatOptionsSelectView.findViewById(R.id.repeat_custom_tuesday);
        CheckBox repeatCustomWedCheckBox =
                repeatOptionsSelectView.findViewById(R.id.repeat_custom_wednesday);
        CheckBox repeatCustomThuCheckBox =
                repeatOptionsSelectView.findViewById(R.id.repeat_custom_thursday);
        CheckBox repeatCustomFriCheckBox =
                repeatOptionsSelectView.findViewById(R.id.repeat_custom_friday);
        CheckBox repeatCustomSatCheckBox =
                repeatOptionsSelectView.findViewById(R.id.repeat_custom_saturday);

        // User below logic to populate the checkbox with right values when Alarm/Reminder are
        // being modified
        if ((repeatInput != Alarm.INVALID_VALUE)) {
            if (repeatInput == Alarm.ALARM_REPEAT_ONCE) {
                repeatOnceCheckBox.setChecked(true);
                repeatEveryOccurCheckBox.setChecked(false);
            } else if (repeatInput == Alarm.ALARM_REPEAT_DAILY) {
                repeatOnceCheckBox.setChecked(false);
                repeatDailyCheckBox.setChecked(true);
            } else if (repeatInput == Alarm.ALARM_REPEAT_EVERY_OCCURRENCE) {
                repeatOnceCheckBox.setChecked(false);
                repeatEveryOccurCheckBox.setChecked(true);
            } else {
                repeatOnceCheckBox.setChecked(false);
                repeatCustomCheckBox.setChecked(true);
                repeatInputLayout.setVisibility(View.VISIBLE);
                String repeatOptionStr = String.valueOf(repeatInput);
                repeatCustomSunCheckBox.setChecked(false);
                repeatCustomMonCheckBox.setChecked(false);
                repeatCustomTueCheckBox.setChecked(false);
                repeatCustomWedCheckBox.setChecked(false);
                repeatCustomThuCheckBox.setChecked(false);
                repeatCustomFriCheckBox.setChecked(false);
                repeatCustomSatCheckBox.setChecked(false);

                // What is the logic here?
                // Each Day is represented by a number - Sun(0), ... ,Sat(6)
                // Why?
                // This way we can save space & also represent the selected locale for the
                // repeat options.
                int len = repeatOptionStr.length();
                int startIndex = 0;
                while (startIndex < len) {
                    int repeatCustomDay =
                            Character.getNumericValue(repeatOptionStr.charAt(startIndex++));
                    if (repeatCustomDay == Alarm.ALARM_REPEAT_SUN) {
                        repeatCustomSunCheckBox.setChecked(true);
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_MON) {
                        repeatCustomMonCheckBox.setChecked(true);
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_TUE) {
                        repeatCustomTueCheckBox.setChecked(true);
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_WED) {
                        repeatCustomWedCheckBox.setChecked(true);
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_THU) {
                        repeatCustomThuCheckBox.setChecked(true);
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_FRI) {
                        repeatCustomFriCheckBox.setChecked(true);
                    }

                    if (repeatCustomDay == Alarm.ALARM_REPEAT_SAT) {
                        repeatCustomSatCheckBox.setChecked(true);
                    }
                }
            }
        }

        // Show Alert Dialog with Repeat Options
        AlertDialog alertDialog =
                new AlertDialog.Builder(this)
                        .setCancelable(true)
                        .setNegativeButton(getString(R.string.cancel), null)
                        .setPositiveButton(getString(R.string.select), (dialog, which) -> {
                            updateVisbilityForMenuItem(true);

                            // Create a repeat string based on day selected (delimiter: space)
                            if (repeatOnceCheckBox.isChecked()) {
                                repeatOptionsText.setText(R.string.repeat_once);
                                repeatOptions = Alarm.ALARM_REPEAT_ONCE;
                            } else if (repeatDailyCheckBox.isChecked()) {
                                repeatOptionsText.setText(R.string.repeat_daily);
                                repeatOptions = Alarm.ALARM_REPEAT_DAILY;
                            } else if (repeatEveryOccurCheckBox.isChecked()) {
                                repeatOptionsText.setText(R.string.repeat_every_occurrence);
                                repeatOptions = Alarm.ALARM_REPEAT_EVERY_OCCURRENCE;
                            } else if (repeatCustomCheckBox.isChecked()) {
                                String repeatOptionStr = "";
                                repeatOptionsText.setText(R.string.repeat_custom);
                                daysSelected = 0;
                                if (repeatCustomMonCheckBox.isChecked()) {
                                    repeatOptionStr += Alarm.ALARM_REPEAT_MON;
                                    daysSelected += 1;
                                }
                                if (repeatCustomTueCheckBox.isChecked()) {
                                    repeatOptionStr += Alarm.ALARM_REPEAT_TUE;
                                    daysSelected += 1;
                                }
                                if (repeatCustomWedCheckBox.isChecked()) {
                                    repeatOptionStr += Alarm.ALARM_REPEAT_WED;
                                    daysSelected += 1;
                                }
                                if (repeatCustomThuCheckBox.isChecked()) {
                                    repeatOptionStr += Alarm.ALARM_REPEAT_THU;
                                    daysSelected += 1;
                                }
                                if (repeatCustomFriCheckBox.isChecked()) {
                                    repeatOptionStr += Alarm.ALARM_REPEAT_FRI;
                                    daysSelected += 1;
                                }
                                if (repeatCustomSatCheckBox.isChecked()) {
                                    repeatOptionStr += Alarm.ALARM_REPEAT_SAT;
                                    daysSelected += 1;
                                }
                                if (repeatCustomSunCheckBox.isChecked()) {
                                    repeatOptionStr += Alarm.ALARM_REPEAT_SUN;
                                    daysSelected += 1;
                                }

                                if (daysSelected == TOTAL_DAYS_IN_WEEK) {
                                    repeatOptionsText.setText(R.string.repeat_daily);
                                    repeatOptions = Alarm.ALARM_REPEAT_DAILY;
                                } else {
                                    repeatOptions = Integer.parseInt(repeatOptionStr);
                                }
                            }
                        })
                        .setView(repeatOptionsSelectView).create();
        alertDialog.getWindow().setBackgroundDrawableResource(R.color.lightSaffron);
        alertDialog.show();
    }
}