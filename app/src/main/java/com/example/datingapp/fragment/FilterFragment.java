package com.example.datingapp.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.datingapp.R;
import com.example.datingapp.dto.request.LocationUpdateDto;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FilterFragment extends Fragment {

    private RangeSlider ageRangeSlider;
    private TextView tvAgeRange;
    private RadioGroup rgGender;
    private Slider distanceSlider;
    private TextView tvDistanceValue;
    private Button btnApplyFilter;

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các thành phần
        ageRangeSlider = view.findViewById(R.id.ageRangeSlider);
        tvAgeRange = view.findViewById(R.id.tvAgeRange);
        rgGender = view.findViewById(R.id.rgGender);
        distanceSlider = view.findViewById(R.id.distanceSlider);
        tvDistanceValue = view.findViewById(R.id.tvDistanceValue);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);

        // Khởi tạo FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Khởi tạo launcher để yêu cầu quyền
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                updateUserLocation();
            } else {
                Toast.makeText(requireContext(), "Quyền truy cập vị trí bị từ chối", Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý RangeSlider cho độ tuổi
        ageRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int minAge = Math.round(values.get(0));
            int maxAge = Math.round(values.get(1));
            tvAgeRange.setText(minAge + " - " + maxAge);
        });

        // Xử lý Slider cho khoảng cách
        distanceSlider.addOnChangeListener((slider, value, fromUser) -> {
            int distance = Math.round(value);
            tvDistanceValue.setText(distance + " km");
        });

        // Kiểm tra và yêu cầu quyền truy cập vị trí
        checkLocationPermission();

        // Xử lý nút Áp dụng
        btnApplyFilter.setOnClickListener(v -> applyFilter());
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            updateUserLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void updateUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Không có quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo LocationRequest để yêu cầu vị trí mới
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Ưu tiên GPS
        locationRequest.setNumUpdates(1); // Chỉ lấy 1 lần cập nhật
        locationRequest.setInterval(0); // Lấy ngay lập tức

        // Kiểm tra xem Location có được bật không
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(requireContext(), "Vui lòng bật GPS để lấy vị trí", Toast.LENGTH_LONG).show();
            // Hướng dẫn người dùng bật GPS
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.e("FilterFragment", "Không nhận được kết quả vị trí");
                    Toast.makeText(requireContext(), "Không thể lấy vị trí", Toast.LENGTH_SHORT).show();
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Log.d("FilterFragment", "Vị trí mới: " + latitude + ", " + longitude);
                    sendLocationToServer(latitude, longitude);
                } else {
                    Log.e("FilterFragment", "Location is null");
                    Toast.makeText(requireContext(), "Không thể lấy vị trí, thử lại sau", Toast.LENGTH_SHORT).show();
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        }, Looper.getMainLooper()).addOnFailureListener(e -> {
            Log.e("FilterFragment", "Lỗi khi yêu cầu vị trí: " + e.getMessage());
            Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void sendLocationToServer(double latitude, double longitude) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        LocationUpdateDto request = new LocationUpdateDto();
        request.setLatitude(latitude);
        request.setLongitude(longitude);

        Call<ApiResponse<Void>> call = authService.updateLocation("Bearer " + getAuthToken(), request);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    Log.d("FilterFragment", "Cập nhật vị trí thành công");
                } else {
                    String errorMessage = "Cập nhật vị trí thất bại";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage += ": " + response.errorBody().string();
                        } catch (Exception e) {
                            errorMessage += ": Lỗi không xác định";
                        }
                    }
                    Log.e("FilterFragment", errorMessage);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("FilterFragment", "Lỗi khi gửi vị trí: " + t.getMessage());
            }
        });
    }

    private String getAuthToken() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("MyPrefs", requireContext().MODE_PRIVATE);
        return sharedPreferences.getString("authToken", null);
    }

    private void applyFilter() {
        // Lấy giá trị từ RangeSlider
        List<Float> ageValues = ageRangeSlider.getValues();
        int minAge = Math.round(ageValues.get(0));
        int maxAge = Math.round(ageValues.get(1));

        // Lấy giá trị từ RadioGroup
        String gender;
        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == R.id.rbMale) {
            gender = "Nam";
        } else if (selectedId == R.id.rbFemale) {
            gender = "Nữ";
        } else {
            gender = "Tất cả";
        }

        // Lấy giá trị từ Slider
        double maxDistance = Math.round(distanceSlider.getValue());

        // Tạo Bundle để gửi dữ liệu
        Bundle bundle = new Bundle();
        bundle.putInt("minAge", minAge);
        bundle.putInt("maxAge", maxAge);
        bundle.putString("gender", gender);
        bundle.putDouble("maxDistance", maxDistance);

        try {
            // Lấy NavController từ activity
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.action_filter_to_profile, bundle);
        } catch (IllegalStateException e) {
            Log.e("FilterFragment", "Lỗi điều hướng: " + e.getMessage());
            Toast.makeText(requireContext(), "Lỗi điều hướng, vui lòng thử lại", Toast.LENGTH_SHORT).show();
        }
    }
}