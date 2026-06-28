package com.hs.hstesis.notifications.domain.services;

import com.hs.hstesis.notifications.domain.model.aggregates.NotificationPreference;
import com.hs.hstesis.notifications.domain.model.commands.UpdateNotificationPreferencesCommand;

public interface NotificationPreferenceCommandService {
    NotificationPreference handle(UpdateNotificationPreferencesCommand command);
}
