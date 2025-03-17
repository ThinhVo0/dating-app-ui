package com.example.datingapp.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.datingapp.R;
import com.example.datingapp.fragment.ChatFragment;
import com.example.datingapp.fragment.FilterFragment;
import com.example.datingapp.fragment.LikeYouFragment;
import com.example.datingapp.fragment.NotificationsFragment;
import com.example.datingapp.fragment.ProfileFragment;
import com.example.datingapp.fragment.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);
        ImageView filterIcon = findViewById(R.id.filter_icon);

        // Xử lý sự kiện click
        filterIcon.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "Bộ lọc đang phát triển", Toast.LENGTH_SHORT).show();
            // TODO: Mở Fragment bộ lọc hoặc hiển thị BottomSheet/Dialog ở đây
            loadFragment(new FilterFragment());
        });
        // Khởi tạo Bottom Navigation View
        BottomNavigationView bottomNavigationView = findViewById(R.id.menu_navigation);

        // Load Fragment mặc định (HomeFragment)
        loadFragment(new ProfileFragment());
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        // Xử lý sự kiện khi chọn item trong Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            // Sử dụng if-else thay vì switch-case để tránh lỗi "constant expression required"
            if (item.getItemId() == R.id.nav_liked) {
                selectedFragment = new LikeYouFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }else if (item.getItemId() == R.id.nav_setting) {
                selectedFragment = new SettingsFragment();
            }else if (item.getItemId() == R.id.nav_notify) {
                selectedFragment = new NotificationsFragment();
            }else if (item.getItemId() == R.id.nav_chat) {
                selectedFragment = new ChatFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }

            return true;
        });
    }

    // Hàm để load Fragment
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}