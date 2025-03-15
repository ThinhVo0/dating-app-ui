package com.example.datingapp.activity;

import android.os.Bundle;
import android.widget.GridView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.datingapp.R;
import com.example.datingapp.adapter.ImageGridAdapter;
import java.util.Arrays;
import java.util.List;

public class LikeYouActivity extends AppCompatActivity {
    private GridView gridView;
    private ImageGridAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.likeyou);

        // Danh sách ảnh từ drawable
        List<Integer> imageList = Arrays.asList(
                R.drawable.image1,
                R.drawable.image2,
                R.drawable.image3,
                R.drawable.image4

        );

        List<Integer> ageList = Arrays.asList(22, 25, 19, 30);


        // Ánh xạ GridView
        gridView = findViewById(R.id.gridViewLikes);

        // Gán adapter cho GridView
        adapter = new ImageGridAdapter(this, imageList, ageList);
        gridView.setAdapter(adapter);
    }
}
