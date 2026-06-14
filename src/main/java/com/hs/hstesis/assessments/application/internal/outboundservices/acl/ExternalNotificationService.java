package com.hs.hstesis.assessments.application.internal.outboundservices.acl;

import com.hs.hstesis.notifications.interfaces.acl.NotificationContextFacade;
import org.springframework.stereotype.Service;

@Service("assessmentsExternalNotificationService")
public class ExternalNotificationService {

    private final NotificationContextFacade notificationContextFacade;

    public ExternalNotificationService(NotificationContextFacade notificationContextFacade) {
        this.notificationContextFacade = notificationContextFacade;
    }

    public void sendNotification(Long userId, String message) {
        notificationContextFacade.createNotification(userId, message);
    }
}
