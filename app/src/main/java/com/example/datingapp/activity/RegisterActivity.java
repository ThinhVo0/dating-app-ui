package com.example.datingapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.datingapp.R;
import com.example.datingapp.dto.request.SignUpDto;
import com.example.datingapp.dto.request.VerifySignUpDto;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.model.User;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameInput, emailInput, passwordInput, otpInput;
    private Button signupButton, verifyOtpButton;
    private TextView backToLoginLink;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo các view
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        otpInput = findViewById(R.id.otpInput);
        signupButton = findViewById(R.id.signupButton);
        verifyOtpButton = findViewById(R.id.verifyOtpButton);
        backToLoginLink = findViewById(R.id.backToLoginLink);

        // Khởi tạo Retrofit client
        authService = RetrofitClient.getClient().create(AuthService.class);

        // Handle Signup button click
        signupButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            } else {
                requestSignup(username, email, password);
            }
        });

        // Handle Verify OTP button click
        verifyOtpButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String otp = otpInput.getText().toString().trim();

            if (otp.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã OTP!", Toast.LENGTH_SHORT).show();
            } else {
                verifySignup(username, email, password, otp);
            }
        });

        // Handle Back to Login click
        backToLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void requestSignup(String username, String email, String password) {
        SignUpDto signUpDto = new SignUpDto();
        signUpDto.setUsername(username);
        signUpDto.setEmail(email);
        signUpDto.setPassword(password);

        Call<ApiResponse<String>> call = authService.requestSignup(signUpDto);
        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    Toast.makeText(RegisterActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    if (apiResponse.getStatus() == 200) {
                        // Chuyển giao diện sang bước nhập OTP
                        signupButton.setVisibility(View.GONE);
                        otpInput.setVisibility(View.VISIBLE);
                        verifyOtpButton.setVisibility(View.VISIBLE);
                        // Vô hiệu hóa các trường nhập để tránh chỉnh sửa
                        usernameInput.setEnabled(false);
                        emailInput.setEnabled(false);
                        passwordInput.setEnabled(false);
                    }
                } else {
                    String errorMsg = "Có lỗi xảy ra!";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                            android.util.Log.e("SignupError", "Error Body: " + errorMsg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                android.util.Log.e("SignupError", "Failure: " + t.getMessage());
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void verifySignup(String username, String email, String password, String otp) {
        VerifySignUpDto verifySignUpDto = new VerifySignUpDto();
        verifySignUpDto.setUsername(username);
        verifySignUpDto.setEmail(email);
        verifySignUpDto.setPassword(password);
        verifySignUpDto.setOtp(otp);

        Call<ApiResponse<User>> call = authService.verifySignup(verifySignUpDto);
        call.enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> apiResponse = response.body();
                    Toast.makeText(RegisterActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    if (apiResponse.getStatus() == 200) {
                        // Đăng ký thành công, chuyển sang màn hình Login
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    String errorMsg = "OTP không hợp lệ hoặc có lỗi!";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                            android.util.Log.e("VerifySignupError", "Error Body: " + errorMsg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                android.util.Log.e("VerifySignupError", "Failure: " + t.getMessage());
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}