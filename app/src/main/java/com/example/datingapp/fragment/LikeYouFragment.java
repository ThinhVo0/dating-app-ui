package com.example.datingapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import com.example.datingapp.R;
import com.example.datingapp.adapter.ImageGridAdapter;
import java.util.Arrays;
import java.util.List;

public class LikeYouFragment extends Fragment {
    private GridView gridView;
    private ImageGridAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate layout cho Fragment
        return inflater.inflate(R.layout.fragment_like_you, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Danh sách ảnh từ drawable
        List<Integer> imageList = Arrays.asList(
                R.drawable.image2,
                R.drawable.image1,
                R.drawable.image3,
                R.drawable.image4
        );

        List<Integer> ageList = Arrays.asList(22, 25, 19, 30);

        // Ánh xạ GridView
        gridView = view.findViewById(R.id.gridViewLikes);

        // Gán adapter cho GridView
        adapter = new ImageGridAdapter(requireContext(), imageList, ageList);
        gridView.setAdapter(adapter);

        // (Tùy chọn) Thêm sự kiện click cho GridView
        gridView.setOnItemClickListener((parent, v, position, id) -> {
            Toast.makeText(requireContext(), "Item " + position + " clicked", Toast.LENGTH_SHORT).show();
        });
    }
}