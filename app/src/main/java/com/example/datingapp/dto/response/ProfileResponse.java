package com.example.datingapp.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class ProfileResponse {

    private String firstName;
    private String lastName;
    private String gender;
    private int age;
    private int height;
    private String bio;
    private String zodiacSign;
    private String personalityType;
    private String communicationStyle;
    private String loveLanguage;
    private String petPreference;
    private String drinkingHabit;
    private String smokingHabit;
    private String sleepingHabit;
    private List<String> hobbies;

    private String street;
    private String district;
    private String province;

    private String pic1;
    private String pic2;
    private String pic3;
    private String pic4;
    private String pic5;
    private String pic6;
    private String pic7;
    private String pic8;
    private String pic9;



    private int status;
    private String message;
    private List<String> data;
}
