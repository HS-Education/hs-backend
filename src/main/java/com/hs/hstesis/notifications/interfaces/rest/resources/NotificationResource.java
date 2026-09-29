package com.hs.hstesis.notifications.interfaces.rest.resources;

import java.util.Date;

public record NotificationResource(
        Long id,
        Long userId,
        String message,
        com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType type,
        boolean isRead,
        Date createdAt
) {}
