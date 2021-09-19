package com.gkmhc.vedanta.nithya_panchangam;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import java.util.Locale;
import java.util.Objects;

/**
 * Activity to handle App Settings.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class NithyaPanchangamSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Let default locale be "English" for Settings activity
        Locale locale = new Locale("EN");
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nithya_panchangam_settings);

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.settings_title);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (findViewById(R.id.settings_frame) != null) {
            if (savedInstanceState != null) {
                return;
            }

            SettingsFragment settingsFragment = new SettingsFragment();
            getFragmentManager().beginTransaction().add(R.id.settings_frame, settingsFragment).commit();
        }
    }
}