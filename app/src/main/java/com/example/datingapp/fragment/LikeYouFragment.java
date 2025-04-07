package com.example.datingapp.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.datingapp.R;
import com.example.datingapp.adapter.ImageGridAdapter;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.UserInfoResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;


import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LikeYouFragment extends Fragment {
    private static final String TAG = "LikeYouFragment";
    private GridView gridView;
    private ImageGridAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_like_you, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gridView = view.findViewById(R.id.gridViewLikes);

        // Lấy token từ SharedPreferences
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", requireContext().MODE_PRIVATE);
        String authToken = sharedPreferences.getString("authToken", null);

        if (authToken == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem danh sách", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gọi API để lấy danh sách người thích bạn
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Call<ApiResponse<List<UserInfoResponse>>> call = authService.getLikedUsers("Bearer " + authToken);

        call.enqueue(new Callback<ApiResponse<List<UserInfoResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UserInfoResponse>>> call, Response<ApiResponse<List<UserInfoResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    List<UserInfoResponse> likedUsers = response.body().getData();
                    if (likedUsers.isEmpty()) {
                        Toast.makeText(requireContext(), "Chưa có ai thích bạn!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Tạo danh sách ảnh và tuổi từ dữ liệu API
                    List<String> imageUrls = new ArrayList<>();
                    List<Integer> ageList = new ArrayList<>();
                    for (UserInfoResponse user : likedUsers) {
                        // Lấy ảnh đầu tiên (pic1) nếu có, nếu không thì dùng ảnh mặc định
                        String pic = user.getPic1() != null && !user.getPic1().isEmpty()
                                ? user.getPic1()
                                : "https://via.placeholder.com/150"; // URL ảnh mặc định
                        imageUrls.add(pic);
                        ageList.add(user.getAge());
                        Log.d(TAG, "Added user: " + user.getFirstName() + " - " + pic);
                    }

                    // Cập nhật adapter với dữ liệu từ API
                    adapter = new ImageGridAdapter(requireContext(), imageUrls, ageList);
                    gridView.setAdapter(adapter);

                    // Sự kiện click (tùy chọn)
                    gridView.setOnItemClickListener((parent, v, position, id) -> {
                        String name = likedUsers.get(position).getFirstName() + " " + likedUsers.get(position).getLastName();
                        Toast.makeText(requireContext(), "Clicked on " + name, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Toast.makeText(requireContext(), "Không thể tải danh sách", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UserInfoResponse>>> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "API call failed: " + t.getMessage());
            }
        });
    }
}