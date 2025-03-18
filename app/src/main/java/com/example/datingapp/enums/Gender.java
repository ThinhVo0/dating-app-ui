package com.example.datingapp.enums;

public enum Gender {
    MALE("Nam"),
    FEMALE("Nữ");

    private final String vietnamese;

    Gender(String vietnamese) {
        this.vietnamese = vietnamese;
    }

    public String getDisplayName() {
        return vietnamese;
    }
    @Override
    public String toString() {
        return vietnamese;
    }

}

