package com.hs.hstesis.notifications.infrastructure.brokers.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class NotificationRealtimeRabbitConfig {

    public static final String EXCHANGE_NAME = "hs.notifications.realtime";

    @Bean
    public TopicExchange notificationRealtimeExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean(name = "notificationRealtimeQueue")
    public Queue notificationRealtimeQueue() {
        return QueueBuilder.nonDurable().exclusive().autoDelete().build();
    }

    @Bean
    public Binding notificationRealtimeBinding(@Qualifier("notificationRealtimeQueue") Queue notificationRealtimeQueue,
                                               TopicExchange notificationRealtimeExchange) {
        return BindingBuilder.bind(notificationRealtimeQueue)
                .to(notificationRealtimeExchange)
                .with("user.*");
    }
}
