package com.hs.hstesis.notifications.domain.model.commands;

public record CreateNotificationCommand(
        Long userId,
        String message,
        com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType type
) {}
