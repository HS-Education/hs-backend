package com.hs.hstesis.notifications.application.internal.outboundservices;

import com.hs.hstesis.notifications.domain.model.aggregates.Notification;

public interface NotificationRealtimePublisher {
    void publish(Notification notification);
}
