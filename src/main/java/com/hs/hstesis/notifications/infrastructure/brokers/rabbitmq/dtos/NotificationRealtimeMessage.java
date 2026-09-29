package com.hs.hstesis.notifications.infrastructure.brokers.rabbitmq.dtos;

import com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType;

import java.util.Date;

public record NotificationRealtimeMessage(
        Long id,
        Long userId,
        String message,
        NotificationType type,
        boolean read,
        Date createdAt
) {
}
