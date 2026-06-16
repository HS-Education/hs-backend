package com.hs.hstesis.notifications.domain.services;

import com.hs.hstesis.notifications.domain.model.commands.CreateNotificationCommand;
import com.hs.hstesis.notifications.domain.model.commands.MarkNotificationAsReadCommand;

public interface NotificationCommandService {
    void handle(CreateNotificationCommand command);
    void handle(MarkNotificationAsReadCommand command);
}
