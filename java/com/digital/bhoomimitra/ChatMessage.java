package com.digital.bhoomimitra;

public class ChatMessage {

    public String id;
    public String sender;
    public String content;
    public String groupName;
    public long timestamp;

    // New fields for Reply feature
    public String replyToId;
    public String replyToSender;
    public String replyToContent;

    public ChatMessage() {}

    public ChatMessage(String sender, String content, String groupName, long timestamp) {
        this.sender = sender;
        this.content = content;
        this.groupName = groupName;
        this.timestamp = timestamp;
    }
}