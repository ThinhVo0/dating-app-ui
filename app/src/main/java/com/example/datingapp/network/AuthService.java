package com.example.datingapp.network;

import android.provider.ContactsContract;

import com.example.datingapp.dto.request.AccessTokenDto;
import com.example.datingapp.dto.request.ForgotPassWordDto;
import com.example.datingapp.dto.request.LocationUpdateDto;
import com.example.datingapp.dto.request.LoginDto;
import com.example.datingapp.dto.request.ResetPasswordDto;
import com.example.datingapp.dto.request.SignUpDto;
import com.example.datingapp.dto.request.VerifySignUpDto;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.UserResponse;
import com.example.datingapp.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;

public interface AuthService {
    @POST("api/auth/login")
    Call<ApiResponse<UserResponse>> login(@Body LoginDto request);

    @POST("api/auth/logout")
    Call<ApiResponse<String>> logout(@Header("Authorization") String token);

    @POST("api/auth/introspect")
    Call<ApiResponse<String>> introspect(@Body AccessTokenDto token);

    @POST("api/auth/signup/request")
    Call<ApiResponse<String>> requestSignup(@Body SignUpDto signUpDto);

    @POST("api/auth/signup/verify")
    Call<ApiResponse<User>> verifySignup(@Body VerifySignUpDto verifySignUpDto);

    @POST("api/auth/forgot-password")
    Call<ApiResponse<String>> forgotPassword(@Body ForgotPassWordDto forgotPassWordDto);

    // Thêm endpoint đặt lại mật khẩu
    @POST("api/auth/reset-password")
    Call<ApiResponse<String>> resetPassword(@Body ResetPasswordDto resetPasswordDto);

    @POST("api/profiles/update-location")
    Call<ApiResponse<Void>> updateLocation(
            @Header("Authorization") String authToken,
            @Body LocationUpdateDto request
    );



    @GET("api/profiles/search")
    Call<ApiResponse<List<ContactsContract.Profile>>> searchProfiles(
            @Query("firstName") String firstName,
            @Query("lastName") String lastName,
            @Query("gender") String gender,
            @Query("age") Integer age,
            @Query("minAge") Integer minAge,
            @Query("maxAge") Integer maxAge,
            @Query("minHeight") Integer minHeight,
            @Query("maxHeight") Integer maxHeight,
            @Query("maxDistance") Double maxDistance,
            @Header("Authorization") String token
    );
}
