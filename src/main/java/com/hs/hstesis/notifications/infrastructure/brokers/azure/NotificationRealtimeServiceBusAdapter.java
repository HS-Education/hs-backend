package com.hs.hstesis.notifications.infrastructure.brokers.azure;

import com.azure.messaging.servicebus.*;
import com.hs.hstesis.notifications.application.internal.outboundservices.NotificationRealtimePublisher;
import com.hs.hstesis.notifications.domain.model.aggregates.Notification;
import com.hs.hstesis.notifications.infrastructure.brokers.rabbitmq.dtos.NotificationRealtimeMessage;
import com.hs.hstesis.notifications.infrastructure.sse.NotificationSseRegistry;
import com.hs.hstesis.repo.infrastructure.brokers.azure.AzureJson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.provider", havingValue = "service-bus")
public class NotificationRealtimeServiceBusAdapter implements NotificationRealtimePublisher, SmartLifecycle {
    private final ServiceBusSenderClient sender;
    private final ServiceBusProcessorClient receiver;
    private final NotificationSseRegistry registry;
    private volatile boolean running;
    public NotificationRealtimeServiceBusAdapter(ServiceBusClientBuilder builder, NotificationSseRegistry registry,
            @Value("${azure.servicebus.notifications-topic}") String topic,
            @Value("${azure.servicebus.notifications-subscription}") String subscription) {
        this.registry = registry;
        sender = builder.sender().topicName(topic).buildClient();
        receiver = builder.processor().topicName(topic).subscriptionName(subscription).maxConcurrentCalls(1)
                .processMessage(context -> registry.publish(AzureJson.read(context.getMessage().getBody().toBytes(), NotificationRealtimeMessage.class)))
                .processError(context -> org.slf4j.LoggerFactory.getLogger(getClass())
                        .warn("Notification relay error; errorType={}", context.getException().getClass().getSimpleName())).buildProcessorClient();
    }
    @Override public void publish(Notification notification) {
        var payload = new NotificationRealtimeMessage(notification.getId(), notification.getUserId(), notification.getMessage(),
                notification.getType(), notification.isRead(), notification.getCreatedAt());
        try { sender.sendMessage(new ServiceBusMessage(AzureJson.write(payload)).setContentType("application/json")
                .setMessageId("notification:" + notification.getId())); }
        catch (RuntimeException e) { registry.publish(payload); }
    }
    @Override public void start() { receiver.start(); running = true; }
    @Override public void stop() { receiver.close(); sender.close(); running = false; }
    @Override public boolean isRunning() { return running; }
}
