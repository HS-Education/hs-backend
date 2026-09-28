package com.hs.hstesis.notifications.interfaces.rest.transform;

import com.hs.hstesis.notifications.domain.model.aggregates.Notification;
import com.hs.hstesis.notifications.interfaces.rest.resources.NotificationResource;

public class NotificationResourceFromEntityAssembler {
    public static NotificationResource toResourceFromEntity(Notification entity) {
        return new NotificationResource(
                entity.getId(),
                entity.getUserId(),
                entity.getMessage(),
                entity.getType(),
                entity.isRead(),
                entity.getCreatedAt()
        );
    }
}
