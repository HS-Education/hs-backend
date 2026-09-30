package com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.listeners;

import com.hs.hstesis.repo.application.internal.commandservices.DocumentStatusTransitionService;
import com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos.DocumentProcessingFailureMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class DocumentProcessingFailureListenerRabbitAdapter {
    private final DocumentStatusTransitionService statusTransitions;

    public DocumentProcessingFailureListenerRabbitAdapter(DocumentStatusTransitionService statusTransitions) {
        this.statusTransitions = statusTransitions;
    }

    @RabbitListener(queues = "document_processing_failed_queue")
    public void receive(DocumentProcessingFailureMessage message) {
        statusTransitions.markFailed(message.documentId());
    }
}
