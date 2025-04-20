package com.example.datingapp.dto;

import lombok.Data;

@Data
public class ConversationSummaryDTO {
    private String userId;
    private String name;
    private String profilePicture;
    private String latestMessage;
    private String latestMessageTime; // ISO format, will be parsed in adapter
    private int unreadCount;
}