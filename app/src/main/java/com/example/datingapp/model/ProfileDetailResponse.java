package com.example.datingapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ProfileDetailResponse {
    private int status;
    private String message;
    private ProfileData data;

    public int getStatus() { return status; }
    public ProfileData getData() { return data; }
}

