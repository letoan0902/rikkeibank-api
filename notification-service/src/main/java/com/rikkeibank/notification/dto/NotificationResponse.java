package com.rikkeibank.notification.dto;

import com.rikkeibank.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, Long customerId, String channel, String title, String content,
                                   String eventType, String transactionCode, LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getCustomerId(), n.getChannel().name(), n.getTitle(),
                n.getContent(), n.getEventType(), n.getTransactionCode(), n.getCreatedAt());
    }
}
