package com.hs.hstesis.notifications.domain.services;

import com.hs.hstesis.notifications.domain.model.aggregates.Notification;
import com.hs.hstesis.notifications.domain.model.queries.GetUnreadNotificationsByUserIdQuery;

import java.util.List;

public interface NotificationQueryService {
    List<Notification> handle(GetUnreadNotificationsByUserIdQuery query);
}
