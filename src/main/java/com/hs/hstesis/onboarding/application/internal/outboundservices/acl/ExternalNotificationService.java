package com.hs.hstesis.onboarding.application.internal.outboundservices.acl;

import com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType;
import com.hs.hstesis.notifications.interfaces.acl.NotificationContextFacade;
import org.springframework.stereotype.Service;

@Service("onboardingExternalNotificationService")
public class ExternalNotificationService {
    private final NotificationContextFacade notificationContextFacade;

    public ExternalNotificationService(NotificationContextFacade notificationContextFacade) {
        this.notificationContextFacade = notificationContextFacade;
    }

    public void sendNewTutorialNotification(Long userId, String tutorialTitle) {
        notificationContextFacade.createNotification(
                userId,
                String.format("Nuevo tutorial publicado: %s", tutorialTitle),
                NotificationType.NEW_TUTORIAL);
    }
}
