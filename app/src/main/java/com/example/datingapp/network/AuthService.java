package com.example.datingapp.network;

import android.provider.ContactsContract;

import com.example.datingapp.dto.request.AccessTokenDto;
import com.example.datingapp.dto.request.LocationUpdateRequest;
import com.example.datingapp.dto.request.LoginDto;
import com.example.datingapp.dto.response.ApiResponse;
import com.example.datingapp.dto.response.UserResponse;

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

    @POST("api/profiles/update-location")
    Call<Void> updateLocation(
            @Header("Authorization") String authToken,
            @Body LocationUpdateRequest request
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
