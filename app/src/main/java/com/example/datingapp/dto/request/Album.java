package com.example.datingapp.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class Album {
    private List<String> imagePaths; // Danh sách đường dẫn ảnh đã upload

}