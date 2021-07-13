package com.gkmhc.vedanta.nithya_panchangam;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import java.util.Objects;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Step 1: Read selected locale from shared preferences
        MainActivity.updateSelLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Step 3: Post Splashscreen & delay main activity by 2secs
        new Handler().postDelayed(() -> {
            Intent spIntent = new Intent(SplashScreen.this, MainActivity.class);
            startActivity(spIntent);
            finish();
        }, 2000);
    }
}