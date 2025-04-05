package com.example.datingapp.model;

public class ProfileActionResponse {
    private int status;
    private String message;
    private String data; // data là String, không phải List<String>

    // Getters và setters
    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getData() {
        return data;
    }
}