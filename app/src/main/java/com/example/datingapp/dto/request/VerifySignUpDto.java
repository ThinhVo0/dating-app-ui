package com.example.datingapp.dto.request;

import lombok.Data;

@Data
public class VerifySignUpDto {
    private String username;
    private String password;
    private String email;
    private String otp;
}