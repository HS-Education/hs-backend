package com.hs.hstesis.notifications.application.internal.queryservices;

import com.hs.hstesis.notifications.domain.model.aggregates.Notification;
import com.hs.hstesis.notifications.domain.model.queries.GetUnreadNotificationsByUserIdQuery;
import com.hs.hstesis.notifications.domain.services.NotificationQueryService;
import com.hs.hstesis.notifications.infrastructure.persistence.jpa.repositories.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public NotificationQueryServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> handle(GetUnreadNotificationsByUserIdQuery query) {
        return notificationRepository.findByUserIdAndIsReadFalse(query.userId());
    }
}
