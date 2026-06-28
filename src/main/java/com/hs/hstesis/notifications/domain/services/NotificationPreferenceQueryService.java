package com.hs.hstesis.notifications.domain.services;

import com.hs.hstesis.notifications.domain.model.aggregates.NotificationPreference;
import com.hs.hstesis.notifications.domain.model.queries.GetNotificationPreferencesQuery;

import java.util.Optional;

public interface NotificationPreferenceQueryService {
    Optional<NotificationPreference> handle(GetNotificationPreferencesQuery query);
}
