package com.example.datingapp.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.datingapp.R;
import com.example.datingapp.activity.LoginActivity;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        tvEditProfile = view.findViewById(R.id.tvEditProfile);
        tvEditAccount = view.findViewById(R.id.tvEditAccount);
        tvPrivacy = view.findViewById(R.id.tvPrivacy);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Xử lý click vào tvEditProfile
        tvEditProfile.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chuyển đến màn hình chỉnh sửa thông tin", Toast.LENGTH_SHORT).show();
            openFragment(new ProfileUpdateFragment());
        });

        // Xử lý click vào tvEditAccount
        tvEditAccount.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chuyển đến màn hình chỉnh sửa tài khoản", Toast.LENGTH_SHORT).show();
            // Tạo và mở fragment EditAccount (nếu có)
            // openFragment(new EditAccountFragment());
        });

        // Xử lý click vào tvPrivacy
        tvPrivacy.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chuyển đến màn hình quyền riêng tư", Toast.LENGTH_SHORT).show();
            // Tạo và mở fragment Privacy (nếu có)
            // openFragment(new PrivacyFragment());
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(requireContext(), "Bật thông báo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Tắt thông báo", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> logout());
    }

    // Phương thức mở fragment mới
    private void openFragment(Fragment fragment) {
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment); // Thay R.id.fragment_container bằng ID của container trong activity_main.xml
        transaction.addToBackStack(null); // Thêm vào back stack để quay lại
        transaction.commit();
    }

    private void logout() {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("authToken", null);

        Log.d("SettingsFragment", "Auth token: " + authToken);

        if (authToken == null) {
            clearToken();
            navigateToLogin();
            return;
        }

        Call<ApiResponse<String>> call = authService.logout("Bearer " + authToken);
        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                Log.d("SettingsFragment", "Response code: " + response.code());
                Log.d("SettingsFragment", "Response body: " + (response.body() != null ? response.body().toString() : "null"));
                if (response.errorBody() != null) {
                    try {
                        Log.d("SettingsFragment", "Error body: " + response.errorBody().string());
                    } catch (IOException e) {
                        Log.e("SettingsFragment", "Error reading errorBody: " + e.getMessage());
                    }
                }

                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    clearToken();
                    navigateToLogin();
                } else {
                    String errorMessage = "Đăng xuất thất bại";
                    if (response.body() != null) {
                        errorMessage += ": " + response.body().getMessage();
                    } else if (response.errorBody() != null) {
                        try {
                            errorMessage += ": " + response.errorBody().string();
                        } catch (IOException e) {
                            errorMessage += ": Lỗi không xác định";
                        }
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e("SettingsFragment", "Logout failed: " + t.getMessage());
                Toast.makeText(requireContext(), "Lỗi khi đăng xuất: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearToken() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("authToken");
        editor.apply();
    }

    private void navigateToLogin() {
        Toast.makeText(requireContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}