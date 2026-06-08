package com.hs.hstesis.notifications.application.internal.commandservices;

import com.hs.hstesis.notifications.domain.model.aggregates.Notification;
import com.hs.hstesis.notifications.domain.model.commands.CreateNotificationCommand;
import com.hs.hstesis.notifications.domain.services.NotificationCommandService;
import com.hs.hstesis.notifications.infrastructure.persistence.jpa.repositories.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private final NotificationRepository notificationRepository;

    public NotificationCommandServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void handle(CreateNotificationCommand command) {
        var notification = new Notification(command.userId(), command.message());
        notificationRepository.save(notification);
    }
}
