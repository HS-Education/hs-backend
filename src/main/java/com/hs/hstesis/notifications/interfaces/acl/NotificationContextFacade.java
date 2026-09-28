package com.hs.hstesis.notifications.interfaces.acl;

import com.hs.hstesis.notifications.domain.model.commands.CreateNotificationCommand;
import com.hs.hstesis.notifications.domain.services.NotificationCommandService;
import org.springframework.stereotype.Component;

@Component
public class NotificationContextFacade {
    
    private final NotificationCommandService notificationCommandService;

    public NotificationContextFacade(NotificationCommandService notificationCommandService) {
        this.notificationCommandService = notificationCommandService;
    }

    public void createNotification(Long userId, String message,
                                   com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType type) {
        notificationCommandService.handle(new CreateNotificationCommand(userId, message, type));
    }
}
