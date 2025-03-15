package com.example.datingapp.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.datingapp.R;
import com.example.datingapp.adapter.ImageAdapter;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ImageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Danh sách ảnh từ drawable
        List<Integer> imageList = Arrays.asList(
                R.drawable.image1,
                R.drawable.image2,
                R.drawable.image3,
                R.drawable.image4
        );

        viewPager = findViewById(R.id.viewPager);
        adapter = new ImageAdapter(this, imageList);
        viewPager.setAdapter(adapter);
    }
}