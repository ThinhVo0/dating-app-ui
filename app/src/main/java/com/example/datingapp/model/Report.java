package com.example.datingapp.model;

import com.example.datingapp.dto.ObjectIdDTO;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Report {
    @SerializedName("id")
    private ObjectIdDTO id;

    @SerializedName("reporterId")
    private ObjectIdDTO reporterId;

    @SerializedName("reportedUserId")
    private ObjectIdDTO reportedUserId;

    private String reason;

    @SerializedName("createdAt")
    private String createdAt;
}