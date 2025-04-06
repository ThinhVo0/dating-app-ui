package com.example.datingapp.network;

import android.provider.ContactsContract;

import com.example.datingapp.dto.request.AccessTokenDto;
import com.example.datingapp.dto.request.Album;
import com.example.datingapp.dto.request.ForgotPassWordDto;
import com.example.datingapp.dto.request.LocationUpdateDto;
import com.example.datingapp.dto.request.LoginDto;
import com.example.datingapp.dto.request.ResetPasswordDto;
import com.example.datingapp.dto.request.SignUpDto;
import com.example.datingapp.dto.request.VerifySignUpDto;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.UserResponse;
import com.example.datingapp.model.ProfileActionResponse;
import com.example.datingapp.model.ProfileDetailResponse;
import com.example.datingapp.dto.response.ProfileResponse;
import com.example.datingapp.model.ProfileUpdateDTO;
import com.example.datingapp.model.User;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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
    Call<ProfileResponse> getProfileIds(
            @Header("Authorization") String authToken,
            @Query("gender") String gender,
            @Query("minAge") Integer minAge,
            @Query("maxAge") Integer maxAge,
            @Query("maxDistance") Double maxDistance
    );
    @GET("api/profiles/{id}")
    Call<ProfileDetailResponse> getProfileDetail(
            @Header("Authorization") String authToken,
            @Path("id") String id
    );

    @POST("api/relationships/{profileId}/skip")
    Call<ProfileActionResponse> skipProfile(
            @Header("Authorization") String authToken,
            @Path("profileId") String profileId
    );

    @POST("api/relationships/{profileId}/like")
    Call<ProfileActionResponse> likeProfile(
            @Header("Authorization") String authToken,
            @Path("profileId") String profileId
    );

    @GET("api/relationships/liked-users")
    Call<ApiResponse<List<ProfileResponse>>> getLikedUsers(
            @Header("Authorization") String authToken
    );

    @PUT("api/profiles/update")
    Call<ApiResponse<Void>> updateProfile(
            @Header("Authorization") String authToken,
            @Body ProfileUpdateDTO profile
    );

    @Multipart
    @POST("api/profiles/images/upload")
    Call<ApiResponse<Album>> uploadImage(
            @Header("Authorization") String token,
            @Part MultipartBody.Part file,
            @Part("position") RequestBody position
    );

    @GET("api/profiles/{userId}")
    Call<ApiResponse<ProfileResponse>> getUserProfile(@Header("Authorization") String token, @Path("userId") String userId);
}
