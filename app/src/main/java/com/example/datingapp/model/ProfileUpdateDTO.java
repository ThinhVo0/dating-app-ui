package com.example.datingapp.model;



import com.example.datingapp.enums.CommunicationStyle;
import com.example.datingapp.enums.DrinkingHabit;
import com.example.datingapp.enums.Gender;
import com.example.datingapp.enums.Hobbies;
import com.example.datingapp.enums.LoveLanguage;
import com.example.datingapp.enums.PersonalityType;
import com.example.datingapp.enums.PetPreference;
import com.example.datingapp.enums.SleepingHabit;
import com.example.datingapp.enums.SmokingHabit;
import com.example.datingapp.enums.ZodiacSign;

import java.util.List;

public class ProfileUpdateDTO {
    private String firstName;
    private String lastName;
    private List<Hobbies> hobbies;
    private Gender gender;
    private Integer age;

    private Integer height;

    private String bio;
    private ZodiacSign zodiacSign;
    private PersonalityType personalityType;
    private CommunicationStyle communicationStyle;
    private LoveLanguage loveLanguage;
    private PetPreference petPreference;
    private DrinkingHabit drinkingHabit;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<Hobbies> getHobbies() {
        return hobbies;
    }

    public void setHobbies(List<Hobbies> hobbies) {
        this.hobbies = hobbies;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public ZodiacSign getZodiacSign() {
        return zodiacSign;
    }

    public void setZodiacSign(ZodiacSign zodiacSign) {
        this.zodiacSign = zodiacSign;
    }

    public PersonalityType getPersonalityType() {
        return personalityType;
    }

    public void setPersonalityType(PersonalityType personalityType) {
        this.personalityType = personalityType;
    }

    public CommunicationStyle getCommunicationStyle() {
        return communicationStyle;
    }

    public void setCommunicationStyle(CommunicationStyle communicationStyle) {
        this.communicationStyle = communicationStyle;
    }

    public LoveLanguage getLoveLanguage() {
        return loveLanguage;
    }

    public void setLoveLanguage(LoveLanguage loveLanguage) {
        this.loveLanguage = loveLanguage;
    }

    public PetPreference getPetPreference() {
        return petPreference;
    }

    public void setPetPreference(PetPreference petPreference) {
        this.petPreference = petPreference;
    }

    public DrinkingHabit getDrinkingHabit() {
        return drinkingHabit;
    }

    public void setDrinkingHabit(DrinkingHabit drinkingHabit) {
        this.drinkingHabit = drinkingHabit;
    }

    public SmokingHabit getSmokingHabit() {
        return smokingHabit;
    }

    public void setSmokingHabit(SmokingHabit smokingHabit) {
        this.smokingHabit = smokingHabit;
    }

    public SleepingHabit getSleepingHabit() {
        return sleepingHabit;
    }

    public void setSleepingHabit(SleepingHabit sleepingHabit) {
        this.sleepingHabit = sleepingHabit;
    }

    private SmokingHabit smokingHabit;
    private SleepingHabit sleepingHabit;
}
