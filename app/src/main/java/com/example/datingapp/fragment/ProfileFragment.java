package com.example.datingapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.example.datingapp.R;
import com.example.datingapp.adapter.ImageAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.Arrays;
import java.util.List;

public class ProfileFragment extends Fragment {
    private ViewPager2 viewPager;
    private ImageAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate layout cho Fragment
        return inflater.inflate(R.layout.main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Danh sách ảnh từ drawable
        List<Integer> imageList = Arrays.asList(
                R.drawable.image1,
                R.drawable.image2,
                R.drawable.image3,
                R.drawable.image4
        );

        // Khởi tạo ViewPager2
        viewPager = view.findViewById(R.id.viewPager);
        adapter = new ImageAdapter(requireContext(), imageList);
        viewPager.setAdapter(adapter);

        // Xử lý nút "Xem chi tiết"
        ImageButton btnViewDetails = view.findViewById(R.id.btnViewDetails);
        btnViewDetails.setOnClickListener(v -> showDetailsBottomSheet());

        // Xử lý nút tương tác
        ImageButton btnDislike = view.findViewById(R.id.btnDislike);
        ImageButton btnLike = view.findViewById(R.id.btnLike);
        ImageButton btnChat = view.findViewById(R.id.btnChat);

        btnDislike.setOnClickListener(v -> Toast.makeText(requireContext(), "Disliked!", Toast.LENGTH_SHORT).show());
        btnLike.setOnClickListener(v -> Toast.makeText(requireContext(), "Liked!", Toast.LENGTH_SHORT).show());
        btnChat.setOnClickListener(v -> Toast.makeText(requireContext(), "Chat clicked!", Toast.LENGTH_SHORT).show());
    }

    private void showDetailsBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_details, null);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }
}