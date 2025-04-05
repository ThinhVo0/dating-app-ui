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
import com.example.datingapp.dto.request.ForgotPassWordDto;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.network.AuthService;
import com.example.datingapp.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button sendResetButton;
    private TextView backToLoginButton;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.emailInput);
        sendResetButton = findViewById(R.id.sendResetButton);
        backToLoginButton = findViewById(R.id.backToLoginButton);

        // Khởi tạo Retrofit client
        authService = RetrofitClient.getClient().create(AuthService.class);

        // Handle "Send Request" button click
        sendResetButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show();
            } else {
                sendOtpRequest(email);
            }
        });

        // Handle "Back to Login" click
        backToLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void sendOtpRequest(String email) {
        ForgotPassWordDto forgotPassWordDto = new ForgotPassWordDto();
        forgotPassWordDto.setEmail(email);

        Call<ApiResponse<String>> call = authService.forgotPassword(forgotPassWordDto);
        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    Toast.makeText(ForgotPasswordActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    if (apiResponse.getStatus() == 200) {
                        // Chuyển sang màn hình ResetPasswordActivity và truyền email
                        Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                        intent.putExtra("email", email); // Truyền email
                        startActivity(intent);
                    }
                } else {
                    String errorMsg = "Có lỗi xảy ra!";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                            android.util.Log.e("ForgotPasswordError", "Error Body: " + errorMsg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(ForgotPasswordActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                android.util.Log.e("ForgotPasswordError", "Failure: " + t.getMessage());
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}