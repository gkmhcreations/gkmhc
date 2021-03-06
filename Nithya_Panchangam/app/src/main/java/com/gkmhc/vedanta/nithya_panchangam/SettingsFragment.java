package com.gkmhc.vedanta.nithya_panchangam;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.Nullable;

/**
 * Settings fragment that initiales & handles all App settings.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class SettingsFragment extends PreferenceFragment {
    public static final String PREF_NP_LOCALE_KEY = "PREF_NP_LOCALE_KEY";
    public static final String PREF_SANKALPAM_TYPE_KEY = "PREF_SANKALPAM_TYPE_KEY";
    public static final String PREF_PANCHANGAM_KEY = "PREF_PANCHANGAM_KEY";
    public static final String PREF_CHAANDRAMANA_CALENDAR_KEY = "PREF_CHAANDRAMANA_CALENDAR_KEY";
    public static final String PREF_AYANAMSA_KEY = "PREF_AYANAMSA_KEY";
    public static final String PREF_LOCATION_DEF_VAL_KEY = "PREF_LOCATION_DEF_VAL_KEY";
    public static final String PREF_LOCATION_SETTINGS_KEY = "PREF_LOCATION_SETTINGS_KEY";
    public static final String PREF_APP_LAUNCH_FIRST_TIME_KEY = "PREF_APP_LAUNCH_FIRST_TIME_KEY";
    public static final String PREF_TIMEFORMAT_KEY = "PREF_TIMEFORMAT_KEY";
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        updateDefaultSummary();

        preferenceChangeListener = (sharedPreferences, key) -> {
            // {key, value} pair for the following preferences:
            // Locale - {"PREF_NP_LOCALE_KEY", "Tamil/Sanskrit/English"} - Check string.xml
            // Panchangam - {"PREF_PANCHANGAM_KEY", "Tamil/Sanskrit/English"} - Check string.xml
            // Location - {"PREF_LOCATION_KEY", "Manual/GPS"}
            // Default Location - {"PREF_LOCATION_DEF_VAL_KEY", "Varanasi"} - Not editable!
            // Sankalpam Type - {"PREF_SANKALPAM_TYPE_KEY", "Shubham / Srardham"} - Check string.xml
            switch (key) {
                case PREF_LOCATION_SETTINGS_KEY:
                    String strLocSettingsType = sharedPreferences.getString(key, "");
                    Preference defValPreference = findPreference(PREF_LOCATION_DEF_VAL_KEY);
                    if (strLocSettingsType.equalsIgnoreCase(getString(R.string.pref_location_manual))) {
                        //getActivity().onBackPressed();
                        defValPreference.setEnabled(true);
                        showManualLocationDialog(sharedPreferences, defValPreference);
                    } else {
                        defValPreference.setEnabled(false);
                    }
                    // Let this fall through!
                case PREF_NP_LOCALE_KEY:
                case PREF_SANKALPAM_TYPE_KEY:
                case PREF_PANCHANGAM_KEY:
                case PREF_CHAANDRAMANA_CALENDAR_KEY:
                case PREF_AYANAMSA_KEY:
                case PREF_TIMEFORMAT_KEY: {
                    Preference preference = findPreference(key);
                    preference.setSummary(sharedPreferences.getString(key, ""));
                    break;
                }
                default:
                    break;
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        updateDefaultSummary();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    private void updateDefaultSummary() {
        try {
            SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
            Preference preference = findPreference(PREF_NP_LOCALE_KEY);
            preference.setSummary(sharedPreferences.getString(PREF_NP_LOCALE_KEY, getString(R.string.pref_def_locale)));

            preference = findPreference(PREF_PANCHANGAM_KEY);
            preference.setSummary(sharedPreferences.getString(PREF_PANCHANGAM_KEY, getString(R.string.pref_def_panchangam)));

            preference = findPreference(PREF_LOCATION_SETTINGS_KEY);
            String strLocSettingsType = sharedPreferences.getString(PREF_LOCATION_SETTINGS_KEY,
                    getString(R.string.pref_location_manual));
            preference.setSummary(strLocSettingsType);

            preference = findPreference(PREF_LOCATION_DEF_VAL_KEY);
            preference.setSummary(sharedPreferences.getString(PREF_LOCATION_DEF_VAL_KEY, getString(R.string.pref_def_location_val)));
            preference.setEnabled(strLocSettingsType.equalsIgnoreCase(getString(R.string.pref_location_manual)));
            preference.setOnPreferenceClickListener(preference1 -> {
                showManualLocationDialog(sharedPreferences, preference1);
                return false;
            });

            preference = findPreference(PREF_SANKALPAM_TYPE_KEY);
            preference.setSummary(sharedPreferences.getString(PREF_SANKALPAM_TYPE_KEY, getString(R.string.pref_def_sankalpam_type)));

            preference = findPreference(PREF_CHAANDRAMANA_CALENDAR_KEY);
            preference.setSummary(sharedPreferences.getString(PREF_CHAANDRAMANA_CALENDAR_KEY, getString(R.string.pref_def_lunar_calendar_type)));

            preference = findPreference(PREF_AYANAMSA_KEY);
            preference.setSummary(sharedPreferences.getString(PREF_AYANAMSA_KEY, getString(R.string.pref_def_ayanamsa)));

            preference = findPreference(PREF_TIMEFORMAT_KEY);
            preference.setSummary(sharedPreferences.getString(PREF_TIMEFORMAT_KEY, getString(R.string.pref_def_timeformat)));
        } catch (Exception e) {
            // Do Nothing
        }
    }

    private void showManualLocationDialog(SharedPreferences sharedPreferences, Preference preference) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View locationSelectView =
                inflater.inflate(R.layout.change_manual_location, null);
        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<>(inflater.getContext(),
                        android.R.layout.simple_list_item_1, MainActivity.placesList);
        AutoCompleteTextView autoCompleteTextView =
                locationSelectView.findViewById(R.id.location_dropbox);
        autoCompleteTextView.setAdapter(arrayAdapter);
        AlertDialog alertDialog =
                new AlertDialog.Builder(inflater.getContext())
                        .setCancelable(true)
                        .setView(locationSelectView).create();
        alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        autoCompleteTextView.setOnItemClickListener((adapterView, view, position, id) -> {
            String locationToUpdate = (String)adapterView.getItemAtPosition(position);
            sharedPreferences.edit().putString(PREF_LOCATION_DEF_VAL_KEY, locationToUpdate).apply();
            alertDialog.dismiss();
            preference.setSummary(sharedPreferences.getString(PREF_LOCATION_DEF_VAL_KEY,
                    getString(R.string.pref_def_location_val)));
        });
        autoCompleteTextView.requestFocus();
        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.show();
    }
}
