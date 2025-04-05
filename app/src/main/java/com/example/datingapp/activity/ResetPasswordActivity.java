package com.example.datingapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.datingapp.R;
import com.example.datingapp.dto.request.ResetPasswordDto;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText otpInput, newPasswordInput, confirmPasswordInput;
    private Button resetPasswordButton;
    private AuthService authService;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resetpassword);

        otpInput = findViewById(R.id.otpInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);

        // Khởi tạo Retrofit client
        authService = RetrofitClient.getClient().create(AuthService.class);

        // Nhận email từ ForgotPasswordActivity
        email = getIntent().getStringExtra("email");
        if (email == null) {
            Toast.makeText(this, "Không tìm thấy email!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Handle "Reset Password" button click
        resetPasswordButton.setOnClickListener(v -> {
            String otp = otpInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (otp.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(ResetPasswordActivity.this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            } else if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(ResetPasswordActivity.this, "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show();
            } else {
                resetPassword(email, otp, newPassword);
            }
        });
    }

    private void resetPassword(String email, String otp, String newPassword) {
        ResetPasswordDto resetPasswordDto = new ResetPasswordDto();
        resetPasswordDto.setEmail(email);
        resetPasswordDto.setOtp(otp);
        resetPasswordDto.setNewPassword(newPassword);

        Call<ApiResponse<String>> call = authService.resetPassword(resetPasswordDto);
        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    Toast.makeText(ResetPasswordActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    if (apiResponse.getStatus() == 200) {
                        // Đặt lại mật khẩu thành công, chuyển về màn hình Login
                        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    String errorMsg = "Có lỗi xảy ra!";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                            android.util.Log.e("ResetPasswordError", "Error Body: " + errorMsg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(ResetPasswordActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                android.util.Log.e("ResetPasswordError", "Failure: " + t.getMessage());
                Toast.makeText(ResetPasswordActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}