package com.example.datingapp.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.datingapp.R;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.ProfileResponse;
import com.example.datingapp.fragment.ChatFragment;
import com.example.datingapp.fragment.LikeYouFragment;
import com.example.datingapp.fragment.NotificationsFragment;
import com.example.datingapp.fragment.ProfileFragment;
import com.example.datingapp.fragment.SettingsFragment;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        // Initialize Retrofit service
        authService = RetrofitClient.getClient().create(AuthService.class);

        // Thiết lập BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.menu_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_liked) {
                selectedFragment = new LikeYouFragment();
            } else if (itemId == R.id.nav_chat) {
                selectedFragment = new ChatFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.nav_notify) {
                selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.nav_setting) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                openFragment(selectedFragment);
            }
            return true;
        });

        // Mở fragment mặc định (ví dụ: ProfileFragment)
        if (savedInstanceState == null) {
            openFragment(new ProfileFragment());
        }

        // Xử lý click vào filter_icon
        ImageView filterIcon = findViewById(R.id.filter_icon);
        filterIcon.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FilterActivity.class));
        });

        // Xử lý click vào FloatingActionButton
        FloatingActionButton fab = findViewById(R.id.fab_center);
        fab.setOnClickListener(v -> {
            openFragment(new ProfileFragment());
            Toast.makeText(MainActivity.this, "Chuyển đến trang hồ sơ!", Toast.LENGTH_SHORT).show();
        });

        // Lấy thông tin hồ sơ người dùng
        fetchUserProfile();
    }

    // Phương thức mở fragment
    private void openFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.addToBackStack(null); // Thêm vào back stack để quay lại
        transaction.commit();
    }

    private void fetchUserProfile() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String authToken = sharedPreferences.getString("authToken", null);
        String userId = sharedPreferences.getString("userId", null);

        if (authToken == null || userId == null) {
            Log.e(TAG, "No auth token or userId found, redirecting to login");
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        Call<ApiResponse<ProfileResponse>> call = authService.getUserProfile("Bearer " + authToken, userId);
        call.enqueue(new Callback<ApiResponse<ProfileResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ProfileResponse>> call, Response<ApiResponse<ProfileResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    ProfileResponse profile = response.body().getData();
                    Log.d(TAG, "Profile fetched successfully: " + profile.getFirstName());

                    // Lưu dữ liệu hồ sơ vào SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("firstName", profile.getFirstName());
                    editor.putString("lastName", profile.getLastName());
                    editor.putString("gender", profile.getGender());
                    editor.putInt("age", profile.getAge());
                    editor.putInt("height", profile.getHeight());
                    editor.putString("bio", profile.getBio());
                    editor.putString("zodiacSign", profile.getZodiacSign());
                    editor.putString("personalityType", profile.getPersonalityType());
                    editor.putString("communicationStyle", profile.getCommunicationStyle());
                    editor.putString("loveLanguage", profile.getLoveLanguage());
                    editor.putString("petPreference", profile.getPetPreference());
                    editor.putString("drinkingHabit", profile.getDrinkingHabit());
                    editor.putString("smokingHabit", profile.getSmokingHabit());
                    editor.putString("sleepingHabit", profile.getSleepingHabit());
                    editor.putString("hobbies", String.join(",", profile.getHobbies()));
                    editor.putString("pic1", profile.getPic1());
                    editor.putString("pic2", profile.getPic2());
                    editor.putString("pic3", profile.getPic3());
                    editor.putString("pic4", profile.getPic4());
                    editor.putString("pic5", profile.getPic5());
                    editor.putString("pic6", profile.getPic6());
                    editor.putString("pic7", profile.getPic7());
                    editor.putString("pic8", profile.getPic8());
                    editor.putString("pic9", profile.getPic9());
                    editor.apply();

                    Log.d(TAG, "Profile data stored in SharedPreferences");
                } else {
                    Log.e(TAG, "Failed to fetch profile: " + response.message());
                    Toast.makeText(MainActivity.this, "Không thể tải thông tin hồ sơ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ProfileResponse>> call, Throwable t) {
                Log.e(TAG, "Error fetching profile: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}