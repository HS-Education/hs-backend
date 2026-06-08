package com.hs.hstesis.notifications.interfaces.rest.resources;

import java.util.Date;

public record NotificationResource(Long id, Long userId, String message, boolean isRead, Date createdAt) {}
