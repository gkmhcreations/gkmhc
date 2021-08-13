package com.gkmhc.vedanta.nithya_panchangam;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
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
    public static final String PREF_LOCATION_SETTINGS_KEY = "PREF_LOCATION_SETTINGS_KEY";
    public static final String PREF_SANKALPAM_TYPE_KEY = "PREF_SANKALPAM_TYPE_KEY";
    public static final String PREF_PANCHANGAM_KEY = "PREF_PANCHANGAM_KEY";
    public static final String PREF_AYANAMSA_KEY = "PREF_AYANAMSA_KEY";
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
                case PREF_NP_LOCALE_KEY:
                case PREF_LOCATION_SETTINGS_KEY:
                case PREF_SANKALPAM_TYPE_KEY:
                case PREF_PANCHANGAM_KEY:
                case PREF_AYANAMSA_KEY: {
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
            preference.setSummary(sharedPreferences.getString(PREF_LOCATION_SETTINGS_KEY, getString(R.string.pref_def_location_type)));

            preference = findPreference(PREF_SANKALPAM_TYPE_KEY);
            preference.setSummary(sharedPreferences.getString(PREF_SANKALPAM_TYPE_KEY, getString(R.string.pref_def_sankalpam_type)));

            preference = findPreference(PREF_AYANAMSA_KEY);
            preference.setSummary(sharedPreferences.getString(PREF_AYANAMSA_KEY, getString(R.string.pref_def_ayanamsa)));
        } catch (Exception e) {
            // Do Nothing
        }
    }
}
