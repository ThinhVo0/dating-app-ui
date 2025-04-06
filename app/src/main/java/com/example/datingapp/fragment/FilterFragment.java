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

import java.util.List;

public class FilterFragment extends Fragment {
    private static final String TAG = "FilterFragment";
    private RangeSlider ageRangeSlider;
    private TextView tvAgeRange;
    private RadioGroup rgGender;
    private Slider distanceSlider; // Thêm Slider cho khoảng cách
    private TextView tvDistanceValue; // Thêm TextView để hiển thị giá trị khoảng cách
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
        distanceSlider = view.findViewById(R.id.distanceSlider); // Ánh xạ Slider
        tvDistanceValue = view.findViewById(R.id.tvDistanceValue); // Ánh xạ TextView
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);

        // Log giá trị ban đầu của RangeSlider
        List<Float> initialValues = ageRangeSlider.getValues();
        Log.d(TAG, "Initial Age Range: " + initialValues.get(0) + " - " + initialValues.get(1));

        ageRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int minAge = Math.round(values.get(0));
            int maxAge = Math.round(values.get(1));
            tvAgeRange.setText(minAge + " - " + maxAge);
            Log.d(TAG, "Age Range changed: " + minAge + " - " + maxAge);
        });

        // Xử lý sự kiện thay đổi giá trị của distanceSlider
        distanceSlider.addOnChangeListener((slider, value, fromUser) -> {
            int distance = Math.round(value);
            tvDistanceValue.setText(distance + " km");
            Log.d(TAG, "Distance changed: " + distance + " km");
        });

        btnApplyFilter.setOnClickListener(v -> {
            List<Float> ageValues = ageRangeSlider.getValues();
            int minAge = Math.round(ageValues.get(0));
            int maxAge = Math.round(ageValues.get(1));

            int selectedId = rgGender.getCheckedRadioButtonId();
            String gender = null;
            if (selectedId == R.id.rbMale) {
                gender = "MALE";
            } else if (selectedId == R.id.rbFemale) {
                gender = "FEMALE";
            }

            float maxDistance = distanceSlider.getValue(); // Lấy giá trị từ distanceSlider

            SharedPreferences filterPrefs = requireContext().getSharedPreferences("FilterPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = filterPrefs.edit();
            editor.putInt("minAge", minAge);
            editor.putInt("maxAge", maxAge);
            editor.putString("gender", gender);
            editor.putFloat("maxDistance", maxDistance); // Lưu maxDistance vào SharedPreferences
            editor.apply();

            Log.d(TAG, "Filter applied - MinAge: " + minAge + ", MaxAge: " + maxAge + ", Gender: " + gender + ", MaxDistance: " + maxDistance + " km");
            Toast.makeText(getContext(), "Đã lưu bộ lọc!", Toast.LENGTH_SHORT).show();

            requireActivity().onBackPressed();
        });
    }
}