package com.example.datingapp.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime; // Nếu dùng minSdk 26

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Tạo Gson với TypeAdapter cho LocalDateTime
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter()) // Thêm TypeAdapter cho LocalDate
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:8080/") // Dùng 10.0.2.2 nếu chạy trên emulator, 192.168.1.11 nếu chạy trên thiết bị
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}