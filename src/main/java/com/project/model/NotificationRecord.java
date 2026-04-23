package com.project.model;

import java.time.LocalDateTime;

public class NotificationRecord {
    private final int notificationId;
    private final Integer caseId;
    private final int recipientUserId;
    private final int sentByUserId;
    private final String senderName;
    private final String message;
    private final String channel;
    private final LocalDateTime createdAt;

    public NotificationRecord(int notificationId, Integer caseId, int recipientUserId, int sentByUserId,
                              String senderName, String message, String channel, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.caseId = caseId;
        this.recipientUserId = recipientUserId;
        this.sentByUserId = sentByUserId;
        this.senderName = senderName;
        this.message = message;
        this.channel = channel;
        this.createdAt = createdAt;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public Integer getCaseId() {
        return caseId;
    }

    public int getRecipientUserId() {
        return recipientUserId;
    }

    public int getSentByUserId() {
        return sentByUserId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public String getChannel() {
        return channel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
