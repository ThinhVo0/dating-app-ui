package com.example.datingapp.dto.request;

import com.google.gson.annotations.SerializedName;

public class IdTokenDto {
    @SerializedName("idToken")
    private String idToken;

    public IdTokenDto() {}

    public IdTokenDto(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}