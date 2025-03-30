package com.example.datingapp.activity;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.datingapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        // Tìm NavHostFragment và thiết lập NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // Liên kết NavController với BottomNavigationView
            BottomNavigationView bottomNavigationView = findViewById(R.id.menu_navigation);
            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            // Xử lý click vào filter_icon để mở FilterFragment
            ImageView filterIcon = findViewById(R.id.filter_icon);
            filterIcon.setOnClickListener(v -> {
                navController.navigate(R.id.nav_filter);
            });
        }
    }
}