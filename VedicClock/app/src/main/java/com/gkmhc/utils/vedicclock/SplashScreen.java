package com.gkmhc.utils.vedicclock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(() -> {
            Intent spIntent = new Intent(SplashScreen.this, MainActivity.class);
            startActivity(spIntent);
            finish();
        }, 2000);
    }
}