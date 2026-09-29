package com.hs.hstesis.assessments.application.internal.outboundservices.acl;

import com.hs.hstesis.notifications.interfaces.acl.NotificationContextFacade;
import org.springframework.stereotype.Service;

@Service("assessmentsExternalNotificationService")
public class ExternalNotificationService {

    private final NotificationContextFacade notificationContextFacade;

    public ExternalNotificationService(NotificationContextFacade notificationContextFacade) {
        this.notificationContextFacade = notificationContextFacade;
    }

    public void sendNotification(Long userId, String message,
                                 com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType type) {
        notificationContextFacade.createNotification(userId, message, type);
    }
}
