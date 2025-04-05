package com.example.datingapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ProfileResponse {
    private int status;
    private String message;
    private List<String> data;

    // Getters
    public int getStatus() { return status; }
    public List<String> getData() { return data; }
}