package com.hs.hstesis.notifications.infrastructure.brokers.rabbitmq;

import com.hs.hstesis.notifications.domain.model.aggregates.Notification;
import com.hs.hstesis.notifications.infrastructure.brokers.rabbitmq.dtos.NotificationRealtimeMessage;
import com.hs.hstesis.notifications.infrastructure.sse.NotificationSseRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationRealtimeRabbitAdapter {

    private static final Logger log = LoggerFactory.getLogger(NotificationRealtimeRabbitAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final NotificationSseRegistry sseRegistry;

    public NotificationRealtimeRabbitAdapter(RabbitTemplate rabbitTemplate,
                                             NotificationSseRegistry sseRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.sseRegistry = sseRegistry;
    }

    public void publish(Notification notification) {
        var message = new NotificationRealtimeMessage(
                notification.getId(),
                notification.getUserId(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt()
        );

        try {
            rabbitTemplate.convertAndSend(
                    NotificationRealtimeRabbitConfig.EXCHANGE_NAME,
                    "user." + notification.getUserId(),
                    message
            );
        } catch (RuntimeException exception) {
            // The HTTP endpoint remains the source of truth; deliver locally as a best effort.
            log.warn("Could not publish notification {} through RabbitMQ", notification.getId(), exception);
            sseRegistry.publish(message);
        }
    }

    @RabbitListener(queues = "#{@notificationRealtimeQueue.name}")
    public void relayToLocalClients(NotificationRealtimeMessage message) {
        sseRegistry.publish(message);
    }
}
