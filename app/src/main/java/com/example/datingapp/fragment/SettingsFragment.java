package com.example.datingapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.datingapp.R;

public class SettingsFragment extends Fragment {

    private TextView tvEditProfile, tvEditAccount, tvPrivacy;
    private Switch switchNotifications;
    private Button btnLogout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các thành phần giao diện
        tvEditProfile = view.findViewById(R.id.tvEditProfile);
        tvEditAccount = view.findViewById(R.id.tvEditAccount);
        tvPrivacy = view.findViewById(R.id.tvPrivacy);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Xử lý sự kiện click cho từng mục
        tvEditProfile.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chuyển đến màn hình chỉnh sửa thông tin", Toast.LENGTH_SHORT).show();
            ProfileUpdateFragment profileUpdateFragment = new ProfileUpdateFragment();
            ((FragmentActivity) v.getContext()).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, profileUpdateFragment) // Thay `fragment_container` bằng ID FrameLayout của bạn
                    .addToBackStack(null) // Cho phép quay lại Fragment trước đó
                    .commit();
        });

        tvEditAccount.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "\"Chuyển đến màn hình chỉnh sửa thông tin", Toast.LENGTH_SHORT).show();
            // TODO: Hiển thị dialog hoặc màn hình chọn ngôn ngữ
        });

        tvPrivacy.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chuyển đến màn hình quyền riêng tư", Toast.LENGTH_SHORT).show();
            // TODO: Điều hướng đến màn hình quyền riêng tư
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(requireContext(), "Bật thông báo", Toast.LENGTH_SHORT).show();
                // TODO: Bật thông báo
            } else {
                Toast.makeText(requireContext(), "Tắt thông báo", Toast.LENGTH_SHORT).show();
                // TODO: Tắt thông báo
            }
        });

        btnLogout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
            // TODO: Xử lý logic đăng xuất (ví dụ: xóa token, quay về màn hình đăng nhập)
        });
    }
}