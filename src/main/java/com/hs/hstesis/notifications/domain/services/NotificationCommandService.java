package com.hs.hstesis.notifications.domain.services;

import com.hs.hstesis.notifications.domain.model.commands.CreateNotificationCommand;

public interface NotificationCommandService {
    void handle(CreateNotificationCommand command);
}
