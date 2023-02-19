package com.gkmhc.utils.vedicclock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gkmhc.utils.VedicCalculator;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final double INVALID_VALUE = -1;
    private static final int MAX_NUM_DIGITS_SUPPORTED = 2;
    private double inputNazhigaiVal = INVALID_VALUE;
    private double inputVinaadiVal = INVALID_VALUE;
    private double inputSunRiseHHVal = INVALID_VALUE;
    private double inputSunRiseMMVal = INVALID_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml(
                "<font color='#FFFFFFFF'>" + getString(R.string.vedic_clock_calculator) + "</font>"));

        EditText minuteEditTextBox = findViewById(R.id.minuteVal);
        EditText hourEditTextBox = findViewById(R.id.hourVal);
        EditText vinaadiEditTextBox = findViewById(R.id.viTimeVal);
        EditText nazhigaiEditTextBox = findViewById(R.id.naTimeVal);
        nazhigaiEditTextBox.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        nazhigaiEditTextBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                toggleCalculateLayout(View.INVISIBLE);
                String val = s.toString();
                if (!val.isEmpty()) {
                    inputNazhigaiVal = Double.parseDouble(val);
                    if (val.length() == MAX_NUM_DIGITS_SUPPORTED) {
                        vinaadiEditTextBox.requestFocus();
                    }
                } else {
                    inputNazhigaiVal = INVALID_VALUE;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        vinaadiEditTextBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                toggleCalculateLayout(View.INVISIBLE);
                String val = s.toString();
                if (!val.isEmpty()) {
                    inputVinaadiVal = Double.parseDouble(val);
                    if (val.length() == MAX_NUM_DIGITS_SUPPORTED) {
                        hourEditTextBox.requestFocus();
                    }
                } else {
                    inputVinaadiVal = INVALID_VALUE;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        hourEditTextBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                toggleCalculateLayout(View.INVISIBLE);
                String val = s.toString();
                if (!val.isEmpty()) {
                    inputSunRiseHHVal = Double.parseDouble(val);
                    if (val.length() == MAX_NUM_DIGITS_SUPPORTED) {
                        minuteEditTextBox.requestFocus();
                    }
                } else {
                    inputSunRiseHHVal = INVALID_VALUE;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        minuteEditTextBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                toggleCalculateLayout(View.INVISIBLE);
                String val = s.toString();
                if (!val.isEmpty()) {
                    inputSunRiseMMVal = Double.parseDouble(val);
                } else {
                    inputSunRiseMMVal = INVALID_VALUE;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        Button calculateButton = findViewById(R.id.calculate_button);
        calculateButton.setOnClickListener(view -> {
            String errText = "";
            if ((inputNazhigaiVal != INVALID_VALUE) &&
                (inputVinaadiVal != INVALID_VALUE) &&
                (inputSunRiseHHVal != INVALID_VALUE) &&
                (inputSunRiseMMVal != INVALID_VALUE)) {
                calculateNaViValues();
                toggleCalculateLayout(View.VISIBLE);
            } else {
                errText = "One (or) more input fields are empty!";
            }

            if (!errText.isEmpty()) {
                Toast.makeText(this, errText, Toast.LENGTH_SHORT).show();
            }
        });
        toggleCalculateLayout(View.INVISIBLE);
    }

    private void calculateNaViValues() {
        try {
            double inputNaViVal = (inputNazhigaiVal + (inputVinaadiVal / 100));
            double inputSunRiseVal = (inputSunRiseHHVal * VedicCalculator.MAX_MINS_IN_AN_HOUR) +
                    inputSunRiseMMVal;

            double calculatedNaViVal = VedicCalculator.getVedicClockNaViValue(inputNaViVal, inputSunRiseVal);
            String calculatedHHMMVal = VedicCalculator.getVedicClockHHMMValue(inputNaViVal, inputSunRiseVal);
            TextView textView = findViewById(R.id.calculated_nazhigai_vinaadi_value);
            textView.setText(String.valueOf(calculatedNaViVal));
            textView = findViewById(R.id.calculated_hh_mm_value);
            textView.setText(calculatedHHMMVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleCalculateLayout(int visibility) {
        RelativeLayout calculateLayout = findViewById(R.id.calc_layout);
        calculateLayout.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            InputMethodManager imm;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }
}