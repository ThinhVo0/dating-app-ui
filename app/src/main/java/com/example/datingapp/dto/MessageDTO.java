package com.example.datingapp.dto;

import com.google.gson.annotations.SerializedName;

public class MessageDTO {
    @SerializedName("id")
    private String id;

    @SerializedName("content")
    private String content;

    @SerializedName("senderId")
    private String senderId;

    @SerializedName("receiverId")
    private String receiverId;

    @SerializedName("sendTime")
    private String sendTime;

    @SerializedName("read")
    private boolean read;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public String getSendTime() { return sendTime; }
    public void setSendTime(String sendTime) { this.sendTime = sendTime; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}