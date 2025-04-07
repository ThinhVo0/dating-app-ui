package com.example.datingapp.dto;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class ObjectIdDTO {
    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("date")
    private String date;

    private String hexString;

}