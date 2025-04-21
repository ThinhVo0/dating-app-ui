package com.example.datingapp.activity;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.datingapp.R;
import com.example.datingapp.dto.request.AccessTokenDto;
import com.example.datingapp.dto.request.LocationUpdateDto;
import com.example.datingapp.dto.request.LoginDto;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.UserResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView forgotPassword, registerLink;

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        forgotPassword = findViewById(R.id.forgotPassword);
        registerLink = findViewById(R.id.registerLink);

        // Khởi tạo FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Khởi tạo launcher để yêu cầu quyền
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                updateUserLocation();
            } else {
                Toast.makeText(this, "Quyền truy cập vị trí bị từ chối", Toast.LENGTH_SHORT).show();
            }
        });

        // Kiểm tra nếu đã có token
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String authToken = sharedPreferences.getString("authToken", null);
        if (authToken != null) {
            checkTokenValidity(authToken);
        }

        // Handle Login button click
        loginButton.setOnClickListener(v -> loginUser());

        // Handle Forgot Password click
        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Handle Register click
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void checkTokenValidity(String authToken) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        AccessTokenDto tokenDto = new AccessTokenDto(authToken);

        Call<ApiResponse<String>> call = authService.introspect(tokenDto);
        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    Log.d("LoginActivity", "Token hợp lệ: " + authToken);
                    // Update location before navigating to MainActivity
                    checkLocationPermission();
                    // Navigate to MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Log.d("LoginActivity", "Token không hợp lệ: " + authToken);
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("authToken");
                    editor.remove("userId");
                    editor.apply();
                    Toast.makeText(LoginActivity.this, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e("LoginActivity", "Lỗi khi kiểm tra token: " + t.getMessage());
                Toast.makeText(LoginActivity.this, "Lỗi khi kiểm tra phiên đăng nhập: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser() {
        String username = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        LoginDto request = new LoginDto();
        request.setUsername(username);
        request.setPassword(password);

        Call<ApiResponse<UserResponse>> call = authService.login(request);
        call.enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<UserResponse> apiResponse = response.body();
                    if (apiResponse.getStatus() == 200) {
                        UserResponse userResponse = apiResponse.getData();
                        String token = userResponse.getToken();
                        String userId = userResponse.getId();

                        // Lưu token và userId vào SharedPreferences
                        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("authToken", token);
                        editor.putString("userId", userId);
                        editor.apply();
                        Log.d("LoginActivity", "Saved token: " + token + ", userId: " + userId);

                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                        // Sau khi đăng nhập thành công, lấy tọa độ
                        checkLocationPermission();

                        // Chuyển đến MainActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Tên đăng nhập hoặc mật khẩu không đúng!", Toast.LENGTH_SHORT).show();
                    Log.e("LoginActivity", "Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("LoginActivity", "Login failed: " + t.getMessage());
            }
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            updateUserLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void updateUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Không có quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setNumUpdates(1);
        locationRequest.setInterval(0);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "Vui lòng bật GPS để lấy vị trí", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.e("LoginActivity", "Không nhận được kết quả vị trí");
                    Toast.makeText(LoginActivity.this, "Không thể lấy vị trí", Toast.LENGTH_SHORT).show();
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Log.d("LoginActivity", "Vị trí mới: " + latitude + ", " + longitude);

                    // Lưu tọa độ vào SharedPreferences
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putFloat("latitude", (float) latitude);
                    editor.putFloat("longitude", (float) longitude);
                    editor.apply();

                    // Gửi tọa độ lên server
                    sendLocationToServer(latitude, longitude);
                } else {
                    Log.e("LoginActivity", "Location is null");
                    Toast.makeText(LoginActivity.this, "Không thể lấy vị trí, thử lại sau", Toast.LENGTH_SHORT).show();
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        }, Looper.getMainLooper()).addOnFailureListener(e -> {
            Log.e("LoginActivity", "Lỗi khi yêu cầu vị trí: " + e.getMessage());
            Toast.makeText(LoginActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void sendLocationToServer(double latitude, double longitude) {
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        LocationUpdateDto request = new LocationUpdateDto();
        request.setLatitude(latitude);
        request.setLongitude(longitude);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String authToken = sharedPreferences.getString("authToken", null);

        Call<ApiResponse<Void>> call = authService.updateLocation("Bearer " + authToken, request);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    Log.d("LoginActivity", "Cập nhật vị trí thành công");
                } else {
                    String errorMessage = "Cập nhật vị trí thất bại";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage += ": " + response.errorBody().string();
                        } catch (Exception e) {
                            errorMessage += ": Lỗi không xác định";
                        }
                    }
                    Log.e("LoginActivity", errorMessage);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("LoginActivity", "Lỗi khi gửi vị trí: " + t.getMessage());
            }
        });
    }
}