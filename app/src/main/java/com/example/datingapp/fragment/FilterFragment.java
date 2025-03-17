package com.example.datingapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.datingapp.R;
import com.google.android.material.slider.RangeSlider;

import java.util.List;

public class FilterFragment extends Fragment {

    private RangeSlider ageRangeSlider;
    private TextView tvAgeRange;
    private RadioGroup rgGender;
    private Button btnApplyFilter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các thành phần
        ageRangeSlider = view.findViewById(R.id.ageRangeSlider);
        tvAgeRange = view.findViewById(R.id.tvAgeRange);
        rgGender = view.findViewById(R.id.rgGender);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);

        // Xử lý RangeSlider
        ageRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int minAge = Math.round(values.get(0));
            int maxAge = Math.round(values.get(1));
            tvAgeRange.setText(minAge + " - " + maxAge);
        });

        // Xử lý nút Áp dụng
        btnApplyFilter.setOnClickListener(v -> {
            // Lấy giá trị từ RangeSlider
            List<Float> ageValues = ageRangeSlider.getValues();
            int minAge = Math.round(ageValues.get(0));
            int maxAge = Math.round(ageValues.get(1));

            // Lấy giá trị từ RadioGroup
            String gender;
            int selectedId = rgGender.getCheckedRadioButtonId();
            if (selectedId == R.id.rbMale) {
                gender = "Nam";
            } else if (selectedId == R.id.rbFemale) {
                gender = "Nữ";
            } else {
                gender = "Tất cả";
            }

            // Tạo Bundle để gửi dữ liệu
            Bundle bundle = new Bundle();
            bundle.putInt("minAge", minAge);
            bundle.putInt("maxAge", maxAge);
            bundle.putString("gender", gender);

            // Điều hướng về fragment trước (ví dụ: ProfileFragment) với dữ liệu
            Navigation.findNavController(v).navigate(R.id.nav_profile, bundle);
        });
    }
}