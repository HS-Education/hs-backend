package com.hs.hstesis.repo.infrastructure.configuration;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter(
                "com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos",
                "com.hs.hstesis.notifications.infrastructure.brokers.rabbitmq.dtos");
    }

    @Bean
    public Queue documentProcessingQueue() {
        return QueueBuilder.durable("document_processing_queue").build();
    }

    @Bean
    public Queue embeddingsReadyQueue() {
        return QueueBuilder.durable("embeddings_ready_queue").build();
    }

    @Bean
    public Queue documentProcessingFailedQueue() {
        return QueueBuilder.durable("document_processing_failed_queue").build();
    }

}
