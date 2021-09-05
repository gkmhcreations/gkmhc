package com.gkmhc.vedanta.nithya_panchangam;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import java.util.Locale;

/**
 * Activity to display splash screen when App/Widget comes up.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class SplashScreen extends AppCompatActivity {
    private static final String PREF_NP_LOCALE_KEY = "PREF_NP_LOCALE_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Step 1: Read selected locale from shared preferences
        String prefLang = readLocaleSettings();
        String selLocale = MainActivity.getLocaleShortStr(prefLang);
        Locale locale = new Locale(selLocale);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Step 3: Post Splashscreen & delay main activity by 2secs
        new Handler().postDelayed(() -> {
            Intent spIntent = new Intent(SplashScreen.this, MainActivity.class);
            startActivity(spIntent);
            finish();
        }, 2000);
    }

    private String readLocaleSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences != null) {
            return sharedPreferences.getString(PREF_NP_LOCALE_KEY, "En");
        }
        return "En";
    }
}