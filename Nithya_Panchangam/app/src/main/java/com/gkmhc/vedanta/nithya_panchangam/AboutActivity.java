package com.gkmhc.vedanta.nithya_panchangam;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.Locale;

/**
 * Activity to display more about the App.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class AboutActivity extends AppCompatActivity {
    private static final String PREF_NP_LOCALE_KEY = "PREF_NP_LOCALE_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Step 1: Read selected locale from shared preferences
        String prefLang = readSettings();
        String selLocale = prefLang.substring(0,2);
        Locale locale = new Locale(selLocale);
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        // Step 2: Update configuration with selected locale as per the supported Android Build
        configuration.setLocale(locale);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N){
            getApplicationContext().createConfigurationContext(configuration);
        } else {
            resources.updateConfiguration(configuration,resources.getDisplayMetrics());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
    }

    private String readSettings () {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences != null) {
            return sharedPreferences.getString(PREF_NP_LOCALE_KEY, "En");
        }

        return "En";
    }
}