package com.hs.hstesis.notifications.application.internal.commandservices;

import com.hs.hstesis.notifications.domain.model.aggregates.Notification;
import com.hs.hstesis.notifications.domain.model.commands.CreateNotificationCommand;
import com.hs.hstesis.notifications.domain.model.commands.MarkNotificationAsReadCommand;
import com.hs.hstesis.notifications.domain.services.NotificationCommandService;
import com.hs.hstesis.notifications.infrastructure.persistence.jpa.repositories.NotificationRepository;
import com.hs.hstesis.notifications.application.internal.outboundservices.NotificationRealtimePublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final com.hs.hstesis.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationRealtimePublisher realtimeAdapter;

    public NotificationCommandServiceImpl(
            NotificationRepository notificationRepository,
            com.hs.hstesis.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository notificationPreferenceRepository,
            NotificationRealtimePublisher realtimeAdapter) {
        this.notificationRepository = notificationRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.realtimeAdapter = realtimeAdapter;
    }

    @Override
    @Transactional
    public void handle(CreateNotificationCommand command) {
        var preference = notificationPreferenceRepository.findByUserId(command.userId())
                .orElseGet(() -> new com.hs.hstesis.notifications.domain.model.aggregates.NotificationPreference(command.userId()));
        if (!preference.isEnabled(command.type())) {
            return;
        }
        var notification = new Notification(command.userId(), command.message(), command.type());
        notificationRepository.save(notification);

        realtimeAdapter.publish(notification);
    }

    @Override
    @Transactional
    public void handle(MarkNotificationAsReadCommand command) {
        var notification = notificationRepository.findById(command.notificationId())
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUserId().equals(command.userId())) {
            throw new IllegalArgumentException("Notification does not belong to the user");
        }
        notification.markAsRead();
        notificationRepository.save(notification);
    }
}
