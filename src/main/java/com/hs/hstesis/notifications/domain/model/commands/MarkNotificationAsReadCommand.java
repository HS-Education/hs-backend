package com.hs.hstesis.notifications.domain.model.commands;

public record MarkNotificationAsReadCommand(Long notificationId, Long userId) {
}
