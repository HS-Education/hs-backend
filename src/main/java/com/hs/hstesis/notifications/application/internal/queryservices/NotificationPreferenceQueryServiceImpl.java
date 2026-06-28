package com.hs.hstesis.notifications.application.internal.queryservices;

import com.hs.hstesis.notifications.domain.model.aggregates.NotificationPreference;
import com.hs.hstesis.notifications.domain.model.queries.GetNotificationPreferencesQuery;
import com.hs.hstesis.notifications.domain.services.NotificationPreferenceQueryService;
import com.hs.hstesis.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificationPreferenceQueryServiceImpl implements NotificationPreferenceQueryService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public NotificationPreferenceQueryServiceImpl(NotificationPreferenceRepository notificationPreferenceRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    @Override
    public Optional<NotificationPreference> handle(GetNotificationPreferencesQuery query) {
        var preference = notificationPreferenceRepository.findByUserId(query.userId());
        if (preference.isEmpty()) {
            // Default preferences
            return Optional.of(new NotificationPreference(query.userId()));
        }
        return preference;
    }
}
