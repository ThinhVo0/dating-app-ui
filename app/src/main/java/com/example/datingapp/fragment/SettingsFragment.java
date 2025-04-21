package com.example.datingapp.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.datingapp.R;
import com.example.datingapp.activity.LoginActivity;
import com.example.datingapp.activity.ProfileUpdateActivity;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private TextView tvNameAge, tvEditAccount, tvPrivacy;
    private SwitchMaterial switchNotifications;
    private Button btnLogout;
    private ImageView ivProfilePic, ivEditPencil;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNameAge = view.findViewById(R.id.tvNameAge);
        tvEditAccount = view.findViewById(R.id.tvEditAccount);
        tvPrivacy = view.findViewById(R.id.tvPrivacy);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        btnLogout = view.findViewById(R.id.btnLogout);
        ivProfilePic = view.findViewById(R.id.ivProfilePic);
        ivEditPencil = view.findViewById(R.id.ivEditPencil);

        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        loadProfileData();

        ivEditPencil.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ProfileUpdateActivity.class));
        });


        tvEditAccount.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chuyển đến màn hình chỉnh sửa tài khoản", Toast.LENGTH_SHORT).show();
        });

        tvPrivacy.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chuyển đến màn hình quyền riêng tư", Toast.LENGTH_SHORT).show();
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("notificationsEnabled", isChecked);
            editor.apply();
        });

        btnLogout.setOnClickListener(v -> logout());

        switchNotifications.setChecked(sharedPreferences.getBoolean("notificationsEnabled", true));
    }

    private void loadProfileData() {
        String lastName = sharedPreferences.getString("lastName", "User");
        int age = sharedPreferences.getInt("age", 20);
        tvNameAge.setText(lastName + ", " + age);

        String pic1 = sharedPreferences.getString("pic1", null);
        if (pic1 != null) {
            Glide.with(this).load(pic1).into(ivProfilePic);
        } else {
            ivProfilePic.setImageResource(R.drawable.avt);
        }
    }

    private void openFragment(Fragment fragment) {
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void logout() {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        String authToken = sharedPreferences.getString("authToken", null);

        if (authToken == null) {
            clearToken();
            navigateToLogin();
            return;
        }

        Call<ApiResponse<String>> call = authService.logout("Bearer " + authToken);
        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
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
                Toast.makeText(requireContext(), "Lỗi khi đăng xuất: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearToken() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}