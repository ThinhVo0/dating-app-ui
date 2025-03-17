package com.example.datingapp.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.datingapp.R;
import com.example.datingapp.fragment.LikeYouFragment;
import com.example.datingapp.fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);
        // Khởi tạo Bottom Navigation View
        BottomNavigationView bottomNavigationView = findViewById(R.id.menu_navigation);

        // Load Fragment mặc định (HomeFragment)
        loadFragment(new ProfileFragment());

        // Xử lý sự kiện khi chọn item trong Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            // Sử dụng if-else thay vì switch-case để tránh lỗi "constant expression required"
            if (item.getItemId() == R.id.nav_liked) {
                selectedFragment = new LikeYouFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
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