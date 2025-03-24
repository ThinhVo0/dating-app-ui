package com.example.datingapp.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserResponse {
    private IdObject id;
    private String username;
    private LocalDateTime date; // Thay String thành LocalDateTime
    private String email;
    private String role;
    private String token;
    private String refreshToken;
    // Class để ánh xạ id từ JSON API
    @Data
    public static class IdObject {
        private long timestamp;
        private LocalDateTime date;
    }
}
