package com.example.datingapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.datingapp.R;
import com.example.datingapp.dto.request.AccessTokenDto;
import com.example.datingapp.dto.request.IdTokenDto;
import com.example.datingapp.dto.request.LocationUpdateDto;
import com.example.datingapp.dto.request.LoginDto;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.UserResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView forgotPassword, registerLink;
    private ImageButton googleLoginButton;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private boolean locationUpdated = false;
    private final Handler locationTimeoutHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d("LoginActivity", "onCreate: Initializing LoginActivity");

        // Kiểm tra Google Play Services
        if (!isGooglePlayServicesAvailable()) {
            Log.e("GoogleSignIn", "onCreate: Google Play Services not available");
            Toast.makeText(this, "Google Play Services không khả dụng", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d("GoogleSignIn", "onCreate: Google Play Services available");

        // Khởi tạo UI
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        forgotPassword = findViewById(R.id.forgotPassword);
        registerLink = findViewById(R.id.registerLink);
        googleLoginButton = findViewById(R.id.googleLoginButton);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Log.d("LoginActivity", "onCreate: UI components initialized");

        // Khởi tạo launcher cho quyền vị trí
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            Log.d("Location", "requestPermissionLauncher: Permission granted=" + isGranted);
            if (isGranted) {
                Log.d("Location", "requestPermissionLauncher: Calling updateUserLocationAndNavigate");
                updateUserLocationAndNavigate();
            } else {
                Log.w("Location", "requestPermissionLauncher: Location permission denied");
                Toast.makeText(this, "Quyền truy cập vị trí bị từ chối", Toast.LENGTH_SHORT).show();
                goToMainActivity();
            }
        });
        Log.d("LoginActivity", "onCreate: Location permission launcher initialized");

        // Kiểm tra token đã lưu
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String authToken = sharedPreferences.getString("authToken", null);
        Log.d("Auth", "onCreate: Checking saved auth token=" + authToken);
        if (authToken != null) {
            Log.d("Auth", "onCreate: Validating saved token");
            checkTokenValidity(authToken);
        } else {
            Log.d("Auth", "onCreate: No saved token found");
        }

        // Sự kiện nút đăng nhập
        loginButton.setOnClickListener(v -> {
            Log.d("Auth", "loginButton: Clicked");
            loginUser();
        });
        Log.d("LoginActivity", "onCreate: Login button listener set");

        // Sự kiện quên mật khẩu và đăng ký
        forgotPassword.setOnClickListener(v -> {
            Log.d("Navigation", "forgotPassword: Navigating to ForgotPasswordActivity");
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
        registerLink.setOnClickListener(v -> {
            Log.d("Navigation", "registerLink: Navigating to RegisterActivity");
            startActivity(new Intent(this, RegisterActivity.class));
        });
        Log.d("LoginActivity", "onCreate: Forgot password and register listeners set");

        // Cấu hình Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        Log.d("GoogleSignIn", "onCreate: GoogleSignInOptions configured with basic options");
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        Log.d("GoogleSignIn", "onCreate: GoogleSignInClient initialized");

        // Khởi tạo launcher cho Google Sign-In
        googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Log.d("GoogleSignIn", "googleSignInLauncher: Result received, code=" + result.getResultCode() + ", RESULT_OK=" + RESULT_OK);
            // Chấp nhận cả resultCode == RESULT_OK (-1) và resultCode == 0 như thành công
            if (result.getResultCode() == RESULT_OK || result.getResultCode() == 0) {
                Log.d("GoogleSignIn", "googleSignInLauncher: Success (RESULT_OK or code 0)");
                Intent data = result.getData();
                if (data == null) {
                    Log.e("GoogleSignIn", "googleSignInLauncher: Intent data is null");
                    Toast.makeText(this, "Dữ liệu Google Sign-In không hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d("GoogleSignIn", "googleSignInLauncher: Intent data available, extras=" + (data.getExtras() != null ? data.getExtras().toString() : "null"));
                try {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    Log.d("GoogleSignIn", "googleSignInLauncher: Task created for GoogleSignInAccount");
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    Log.d("GoogleSignIn", "googleSignInLauncher: Google Sign-In successful, email=" + account.getEmail() + ", id=" + account.getId());
                    handleGoogleSignInResult(account);
                } catch (ApiException e) {
                    String errorMessage;
                    switch (e.getStatusCode()) {
                        case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                            errorMessage = "Đăng nhập bị hủy";
                            break;
                        case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                            errorMessage = "Đăng nhập Google thất bại";
                            break;
                        case GoogleSignInStatusCodes.NETWORK_ERROR:
                            errorMessage = "Lỗi mạng, vui lòng kiểm tra kết nối";
                            break;
                        case GoogleSignInStatusCodes.DEVELOPER_ERROR:
                            errorMessage = "Lỗi cấu hình Google Sign-In (DEVELOPER_ERROR)";
                            Log.e("GoogleSignIn", "googleSignInLauncher: Developer error - check OAuth configuration", e);
                            break;
                        default:
                            errorMessage = "Lỗi đăng nhập Google: " + e.getMessage();
                            break;
                    }
                    Log.e("GoogleSignIn", "googleSignInLauncher: ApiException, message=" + errorMessage + ", statusCode=" + e.getStatusCode(), e);
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("GoogleSignIn", "googleSignInLauncher: Unexpected error", e);
                    Toast.makeText(this, "Lỗi không xác định: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w("GoogleSignIn", "googleSignInLauncher: Failed, resultCode=" + result.getResultCode());
                Toast.makeText(this, "Đăng nhập Google không thành công, resultCode=" + result.getResultCode(), Toast.LENGTH_SHORT).show();
            }
        });
        Log.d("LoginActivity", "onCreate: Google Sign-In launcher initialized");

        // Sự kiện nút đăng nhập Google
        googleLoginButton.setOnClickListener(v -> {
            Log.d("GoogleSignIn", "googleLoginButton: Starting Google Sign-In intent");
            
            // Đăng xuất khỏi tài khoản Google hiện tại trước khi bắt đầu quy trình đăng nhập
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Log.d("GoogleSignIn", "googleLoginButton: Signed out from previous Google account");
                // Tạo intent với tùy chọn hiển thị chọn tài khoản
                Intent signInIntent = googleSignInClient.getSignInIntent();
                Bundle options = new Bundle();
                options.putBoolean("com.google.android.gms.auth.FORCE_ACCOUNT_SELECTION", true);
                signInIntent.putExtras(options);
                googleSignInLauncher.launch(signInIntent);
            });
        });
        Log.d("LoginActivity", "onCreate: Google login button listener set");
    }

    // Kiểm tra Google Play Services
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        Log.d("GoogleSignIn", "isGooglePlayServicesAvailable: resultCode=" + resultCode);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                Log.w("GoogleSignIn", "isGooglePlayServicesAvailable: User resolvable error, code=" + resultCode);
                googleApiAvailability.getErrorDialog(this, resultCode, 9000).show();
            } else {
                Log.e("GoogleSignIn", "isGooglePlayServicesAvailable: Google Play Services not supported");
            }
            return false;
        }
        return true;
    }

    private void handleGoogleSignInResult(GoogleSignInAccount account) {
        Log.d("GoogleSignIn", "handleGoogleSignInResult: Processing account, account=" + (account != null));
        if (account != null) {
            String email = account.getEmail();
            String displayName = account.getDisplayName();
            Log.d("GoogleSignIn", "handleGoogleSignInResult: Email=" + email);
            Log.d("GoogleSignIn", "handleGoogleSignInResult: DisplayName=" + displayName);
            if (email != null) {
                Log.d("GoogleSignIn", "handleGoogleSignInResult: Sending email to backend");
                loginWithGoogleEmail(email, displayName);
            } else {
                Log.e("GoogleSignIn", "handleGoogleSignInResult: Email is null");
                Toast.makeText(this, "Không lấy được email từ Google", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("GoogleSignIn", "handleGoogleSignInResult: Google account is null");
            Toast.makeText(this, "Không lấy được thông tin tài khoản Google", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginWithGoogleEmail(String email, String displayName) {
        Log.d("GoogleSignIn", "loginWithGoogleEmail: Email=" + email + ", Name=" + displayName);
        
        // Tạo đối tượng LoginDto với flag googleLogin
        LoginDto request = new LoginDto();
        request.setUsername(email);
        request.setPassword(""); // Mật khẩu trống, backend sẽ xử lý dựa vào flag googleLogin
        request.setGoogleLogin(true);
        
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Log.d("GoogleSignIn", "loginWithGoogleEmail: API call initiated");
        
        authService.login(request).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                Log.d("GoogleSignIn", "loginWithGoogleEmail: Response received, code=" + response.code());
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    UserResponse user = response.body().getData();
                    if (user == null) {
                        Log.e("GoogleSignIn", "loginWithGoogleEmail: UserResponse data is null");
                        Toast.makeText(LoginActivity.this, "Dữ liệu người dùng không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
                    editor.putString("authToken", user.getToken());
                    editor.putString("userId", user.getId());
                    editor.apply();
                    Log.d("GoogleSignIn", "loginWithGoogleEmail: Token saved, userId=" + user.getId());

                    Toast.makeText(LoginActivity.this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                    Log.d("GoogleSignIn", "loginWithGoogleEmail: Checking location permission");
                    checkLocationPermission();
                } else {
                    String errorMessage = response.body() != null ? response.body().getMessage() : "Xác thực Google thất bại";
                    Log.e("GoogleSignIn", "loginWithGoogleEmail: Failed, message=" + errorMessage + ", code=" + response.code());
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                Log.e("GoogleSignIn", "loginWithGoogleEmail: Network error, message=" + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkTokenValidity(String authToken) {
        Log.d("Auth", "checkTokenValidity: Checking token validity, token=" + authToken);
        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Call<ApiResponse<String>> call = authService.introspect(new AccessTokenDto(authToken));
        Log.d("Auth", "checkTokenValidity: API call initiated");

        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                Log.d("Auth", "checkTokenValidity: Response received, code=" + response.code());
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    Log.d("Auth", "checkTokenValidity: Token is valid");
                    checkLocationPermission();
                } else {
                    Log.w("Auth", "checkTokenValidity: Token invalid or expired, code=" + response.code());
                    clearAuthToken();
                    Toast.makeText(LoginActivity.this, "Phiên đăng nhập đã hết hạn", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Log.e("Auth", "checkTokenValidity: Network error, message=" + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Lỗi khi kiểm tra token", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser() {
        Log.d("Auth", "loginUser: Starting login process");
        String username = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        Log.d("Auth", "loginUser: Username=" + username + ", Password length=" + password.length());

        if (username.isEmpty() || password.isEmpty()) {
            Log.w("Auth", "loginUser: Empty username or password");
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("Auth", "loginUser: Preparing login request");
        LoginDto request = new LoginDto();
        request.setUsername(username);
        request.setPassword(password);

        AuthService authService = RetrofitClient.getClient().create(AuthService.class);
        Log.d("Auth", "loginUser: Initiating API call");
        authService.login(request).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                Log.d("Auth", "loginUser: Response received, code=" + response.code());
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 200) {
                    UserResponse user = response.body().getData();
                    if (user == null) {
                        Log.e("Auth", "loginUser: UserResponse data is null");
                        Toast.makeText(LoginActivity.this, "Dữ liệu người dùng không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
                    editor.putString("authToken", user.getToken());
                    editor.putString("userId", user.getId());
                    editor.apply();
                    Log.d("Auth", "loginUser: Token saved, userId=" + user.getId());

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    Log.d("Auth", "loginUser: Checking location permission");
                    checkLocationPermission();
                } else {
                    String errorMessage = response.body() != null ? response.body().getMessage() : "Sai tài khoản hoặc mật khẩu";
                    Log.e("Auth", "loginUser: Failed, message=" + errorMessage + ", code=" + response.code());
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {
                Log.e("Auth", "loginUser: Network error, message=" + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkLocationPermission() {
        Log.d("Location", "checkLocationPermission: Checking location permission");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("Location", "checkLocationPermission: Permission granted");
            updateUserLocationAndNavigate();
        } else {
            Log.d("Location", "checkLocationPermission: Requesting permission");
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void updateUserLocationAndNavigate() {
        Log.d("Location", "updateUserLocationAndNavigate: Starting location update");
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.w("Location", "updateUserLocationAndNavigate: GPS is disabled");
            Toast.makeText(this, "Vui lòng bật GPS để tiếp tục", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }
        Log.d("Location", "updateUserLocationAndNavigate: GPS is enabled");

        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(0)
                .setNumUpdates(1);
        Log.d("Location", "updateUserLocationAndNavigate: LocationRequest created");

        fusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Log.d("Location", "onLocationResult: Location result received");
                Location location = result.getLastLocation();
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();

                    SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
                    editor.putFloat("latitude", (float) lat);
                    editor.putFloat("longitude", (float) lon);
                    editor.apply();
                    Log.d("Location", "onLocationResult: Location saved, lat=" + lat + ", lon=" + lon);

                    Log.d("Location", "onLocationResult: Sending location to server");
                    sendLocationToServer(lat, lon);
                } else {
                    Log.w("Location", "onLocationResult: Location is null");
                }
                locationUpdated = true;
                Log.d("Location", "onLocationResult: Navigating to MainActivity");
                goToMainActivity();
            }
        }, Looper.getMainLooper());
        Log.d("Location", "updateUserLocationAndNavigate: Location updates requested");

        // Timeout sau 5s nếu không có vị trí
        locationTimeoutHandler.postDelayed(() -> {
            if (!locationUpdated) {
                Log.w("Location", "updateUserLocationAndNavigate: Location update timed out");
                Toast.makeText(this, "Không lấy được vị trí, tiếp tục vào app", Toast.LENGTH_SHORT).show();
                goToMainActivity();
            }
        }, 5000);
        Log.d("Location", "updateUserLocationAndNavigate: Timeout handler set for 5 seconds");
    }

    private void sendLocationToServer(double lat, double lon) {
        Log.d("Location", "sendLocationToServer: Sending location, lat=" + lat + ", lon=" + lon);
        String token = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("authToken", null);
        Log.d("Location", "sendLocationToServer: Auth token=" + token);
        if (token == null) {
            Log.e("Location", "sendLocationToServer: Auth token is null, cannot send location");
            return;
        }

        LocationUpdateDto dto = new LocationUpdateDto();
        dto.setLatitude(lat);
        dto.setLongitude(lon);
        Log.d("Location", "sendLocationToServer: LocationUpdateDto created");

        RetrofitClient.getClient().create(AuthService.class)
                .updateLocation("Bearer " + token, dto)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        Log.d("Location", "sendLocationToServer: Response received, code=" + response.code());
                        if (response.isSuccessful()) {
                            Log.d("Location", "sendLocationToServer: Location sent successfully");
                        } else {
                            Log.e("Location", "sendLocationToServer: Failed, code=" + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Log.e("Location", "sendLocationToServer: Network error, message=" + t.getMessage(), t);
                    }
                });
        Log.d("Location", "sendLocationToServer: API call initiated");
    }

    private void goToMainActivity() {
        Log.d("Navigation", "goToMainActivity: Navigating to MainActivity");
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        Log.d("Navigation", "goToMainActivity: Activity finished");
    }

    private void clearAuthToken() {
        Log.d("Auth", "clearAuthToken: Clearing auth token");
        SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
        editor.remove("authToken");
        editor.remove("userId");
        editor.apply();
        Log.d("Auth", "clearAuthToken: Auth token cleared");
    }

    // Xóa hoàn toàn kết nối với tài khoản Google
    private void revokeGoogleAccess() {
        googleSignInClient.revokeAccess()
                .addOnCompleteListener(this, task -> {
                    Log.d("GoogleSignIn", "revokeGoogleAccess: Access revoked successfully");
                    // Có thể thêm xử lý khác ở đây nếu cần
                })
                .addOnFailureListener(this, e -> {
                    Log.e("GoogleSignIn", "revokeGoogleAccess: Failed to revoke access", e);
                });
    }

    // Có thể gọi phương thức này trong onDestroy() nếu muốn xóa kết nối khi thoát
    // hoặc gọi khi người dùng đăng xuất
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Uncomment dòng dưới nếu muốn xóa quyền truy cập khi thoát app
        // revokeGoogleAccess();
    }
}