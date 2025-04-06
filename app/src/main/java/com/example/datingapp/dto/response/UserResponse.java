package com.example.datingapp.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String username;
    private LocalDateTime date; // Thay String thành LocalDateTime
    private String email;
    private String role;
    private String token;
    private String refreshToken;
}
