package com.example.datingapp.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.datingapp.R;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import java.util.Arrays;
import java.util.List;

public class FilterFragment extends Fragment {
    private static final String TAG = "FilterFragment";
    private RangeSlider ageRangeSlider;
    private TextView tvAgeRange;
    private RadioGroup rgGender;
    private Slider distanceSlider;
    private TextView tvDistanceValue;
    private Button btnApplyFilter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ageRangeSlider = view.findViewById(R.id.ageRangeSlider);
        tvAgeRange = view.findViewById(R.id.tvAgeRange);
        rgGender = view.findViewById(R.id.rgGender);
        distanceSlider = view.findViewById(R.id.distanceSlider);
        tvDistanceValue = view.findViewById(R.id.tvDistanceValue);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);

        // Tải dữ liệu từ SharedPreferences
        SharedPreferences filterPrefs = requireContext().getSharedPreferences("FilterPrefs", Context.MODE_PRIVATE);
        int minAge = filterPrefs.getInt("minAge", 18); // Giá trị mặc định
        int maxAge = filterPrefs.getInt("maxAge", 100);
        String gender = filterPrefs.getString("gender", null);
        float maxDistance = filterPrefs.getFloat("maxDistance", 10.0f);

        // Cập nhật UI với giá trị từ SharedPreferences
        ageRangeSlider.setValues((float) minAge, (float) maxAge);
        tvAgeRange.setText(minAge + " - " + maxAge);
        Log.d(TAG, "Loaded Age Range: " + minAge + " - " + maxAge);

        if ("MALE".equals(gender)) {
            rgGender.check(R.id.rbMale);
        } else if ("FEMALE".equals(gender)) {
            rgGender.check(R.id.rbFemale);
        } else {
            rgGender.clearCheck();
        }
        Log.d(TAG, "Loaded Gender: " + gender);

        distanceSlider.setValue(maxDistance);
        tvDistanceValue.setText(Math.round(maxDistance) + " km");
        Log.d(TAG, "Loaded Distance: " + maxDistance + " km");

        // Xử lý sự kiện thay đổi RangeSlider
        ageRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int minAgeValue = Math.round(values.get(0));
            int maxAgeValue = Math.round(values.get(1));
            tvAgeRange.setText(minAgeValue + " - " + maxAgeValue);
            Log.d(TAG, "Age Range changed: " + minAgeValue + " - " + maxAgeValue);
        });

        // Xử lý sự kiện thay đổi Slider khoảng cách
        distanceSlider.addOnChangeListener((slider, value, fromUser) -> {
            int distance = Math.round(value);
            tvDistanceValue.setText(distance + " km");
            Log.d(TAG, "Distance changed: " + distance + " km");
        });

        // Xử lý nút Áp dụng
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
            }

            float maxDistanceValue = distanceSlider.getValue();

            // Lưu vào SharedPreferences
            SharedPreferences.Editor editor = filterPrefs.edit();
            editor.putInt("minAge", minAgeValue);
            editor.putInt("maxAge", maxAgeValue);
            editor.putString("gender", genderValue);
            editor.putFloat("maxDistance", maxDistanceValue);
            editor.apply();

            Log.d(TAG, "Filter applied - MinAge: " + minAgeValue + ", MaxAge: " + maxAgeValue + ", Gender: " + genderValue + ", MaxDistance: " + maxDistanceValue + " km");
            Toast.makeText(getContext(), "Đã lưu bộ lọc!", Toast.LENGTH_SHORT).show();

            // Quay lại và làm mới ProfileFragment
            requireActivity().onBackPressed();
        });
    }
}