package com.example.datingapp.model;


import com.example.datingapp.dto.response.UserResponse;
import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;

import lombok.Data;

@Data
public class User {
    @SerializedName("id")
    private UserResponse.IdObject id;

    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password;

    @SerializedName("email")
    private String email;

    @SerializedName("createAt")
    private LocalDate createAt; // Nếu minSdk < 26, dùng String

    @SerializedName("role")
    private String role;

    @SerializedName("accountStatus")
    private String accountStatus;

    @SerializedName("profileId")
    private UserResponse.IdObject profileId;

    @SerializedName("provider")
    private String provider;

    @SerializedName("subscriptionStatus")
    private String subscriptionStatus;


}