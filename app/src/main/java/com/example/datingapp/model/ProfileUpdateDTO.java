package com.example.datingapp.model;

import com.example.datingapp.enums.*;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Data;

@Data
public class ProfileUpdateDTO {
    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("hobbies")
    private List<String> hobbies; // List<String> thay vì List<Hobbies>

    @SerializedName("gender")
    private String gender; // String thay vì Gender

    @SerializedName("age")
    private int age;

    @SerializedName("height")
    private int height;

    @SerializedName("bio")
    private String bio;

    @SerializedName("zodiacSign")
    private String zodiacSign;

    @SerializedName("personalityType")
    private String personalityType;

    @SerializedName("communicationStyle")
    private String communicationStyle;

    @SerializedName("loveLanguage")
    private String loveLanguage;

    @SerializedName("petPreference")
    private String petPreference;

    @SerializedName("drinkingHabit")
    private String drinkingHabit;

    @SerializedName("smokingHabit")
    private String smokingHabit;

    @SerializedName("sleepingHabit")
    private String sleepingHabit;


}