package com.example.datingapp.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Tạo Gson với TypeAdapter và serialize enum bằng name()
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())// Serialize enum bằng name()
                    .create();
            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

            // Thêm logging interceptor để debug (tùy chọn)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY); // Log toàn bộ request và response
            httpClientBuilder.addInterceptor(logging);

            // Tăng thời gian timeout
            httpClientBuilder.connectTimeout(30, TimeUnit.SECONDS); // Thời gian kết nối tối đa
            httpClientBuilder.readTimeout(30, TimeUnit.SECONDS);    // Thời gian đọc dữ liệu tối đa
            httpClientBuilder.writeTimeout(30, TimeUnit.SECONDS);   // Thời gian ghi dữ liệu tối đa

            // Tạo OkHttpClient
            OkHttpClient client = httpClientBuilder.build();
            retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:8080/") // Dùng 10.0.2.2 nếu chạy trên emulator
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}