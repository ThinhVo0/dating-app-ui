package com.example.datingapp.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.datingapp.R;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import java.util.List;

public class FilterActivity extends AppCompatActivity {
    private static final String TAG = "FilterActivity";
    private RangeSlider ageRangeSlider;
    private TextView tvAgeRange;
    private RadioGroup rgGender;
    private Slider distanceSlider;
    private TextView tvDistanceValue;
    private Button btnApplyFilter;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        ageRangeSlider = findViewById(R.id.ageRangeSlider);
        tvAgeRange = findViewById(R.id.tvAgeRange);
        rgGender = findViewById(R.id.rgGender);
        distanceSlider = findViewById(R.id.distanceSlider);
        tvDistanceValue = findViewById(R.id.tvDistanceValue);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);

        // Back button listener
        btnBack.setOnClickListener(v -> finish());

        // Load data from SharedPreferences
        SharedPreferences filterPrefs = getSharedPreferences("FilterPrefs", Context.MODE_PRIVATE);
        int minAge = filterPrefs.getInt("minAge", 18); // Default value
        int maxAge = filterPrefs.getInt("maxAge", 100);
        String gender = filterPrefs.getString("gender", null);
        float maxDistance = filterPrefs.getFloat("maxDistance", 10.0f);

        // Update UI with SharedPreferences values
        ageRangeSlider.setValues((float) minAge, (float) maxAge);
        tvAgeRange.setText(minAge + " - " + maxAge);
        Log.d(TAG, "Loaded Age Range: " + minAge + " - " + maxAge);

        if ("MALE".equals(gender)) {
            rgGender.check(R.id.rbMale);
        } else if ("FEMALE".equals(gender)) {
            rgGender.check(R.id.rbFemale);
        } else {
            rgGender.check(R.id.rbAll);
        }
        Log.d(TAG, "Loaded Gender: " + gender);

        distanceSlider.setValue(maxDistance);
        tvDistanceValue.setText(Math.round(maxDistance) + " km");
        Log.d(TAG, "Loaded Distance: " + maxDistance + " km");

        // Handle RangeSlider changes
        ageRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int minAgeValue = Math.round(values.get(0));
            int maxAgeValue = Math.round(values.get(1));
            tvAgeRange.setText(minAgeValue + " - " + maxAgeValue);
            Log.d(TAG, "Age Range changed: " + minAgeValue + " - " + maxAgeValue);
        });

        // Handle Distance Slider changes
        distanceSlider.addOnChangeListener((slider, value, fromUser) -> {
            int distance = Math.round(value);
            tvDistanceValue.setText(distance + " km");
            Log.d(TAG, "Distance changed: " + distance + " km");
        });

        // Handle Apply button
        btnApplyFilter.setOnClickListener(v -> {
            List<Float> ageValues = ageRangeSlider.getValues();
            int minAgeValue = Math.round(ageValues.get(0));
            int maxAgeValue = Math.round(ageValues.get(1));

            int selectedId = rgGender.getCheckedRadioButtonId();
            String genderValue = null;
            if (selectedId == R.id.rbMale) {
                genderValue = "MALE";
            } else if (selectedId == R.id.rbFemale) {
                genderValue = "FEMALE";
            } else if (selectedId == R.id.rbAll) {
                genderValue = null; // All
            }

            float maxDistanceValue = distanceSlider.getValue();

            // Save to SharedPreferences
            SharedPreferences.Editor editor = filterPrefs.edit();
            editor.putInt("minAge", minAgeValue);
            editor.putInt("maxAge", maxAgeValue);
            editor.putString("gender", genderValue);
            editor.putFloat("maxDistance", maxDistanceValue);
            editor.apply();

            Log.d(TAG, "Filter applied - MinAge: " + minAgeValue + ", MaxAge: " + maxAgeValue + ", Gender: " + genderValue + ", MaxDistance: " + maxDistanceValue + " km");
            Toast.makeText(this, "Đã lưu bộ lọc!", Toast.LENGTH_SHORT).show();

            // Return to previous screen
            finish();
        });
    }
}