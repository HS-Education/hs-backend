package com.hs.hstesis.repo.infrastructure.brokers.azure;

import com.azure.messaging.servicebus.*;
import com.hs.hstesis.repo.application.internal.outboundservices.messaging.DocumentProcessingPublisher;
import com.hs.hstesis.repo.domain.exceptions.MessageBrokerUnavailableException;
import com.hs.hstesis.repo.domain.model.events.DocumentUploadedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.provider", havingValue = "service-bus")
public class DocumentProcessingServiceBusAdapter implements DocumentProcessingPublisher, AutoCloseable {
    private final ServiceBusSenderClient sender;
    public DocumentProcessingServiceBusAdapter(ServiceBusClientBuilder builder,
            @Value("${azure.servicebus.processing-queue}") String queue) {
        sender = builder.sender().queueName(queue).buildClient();
    }
    @Override
    public void publish(DocumentUploadedEvent event) {
        try {
            var body = java.util.Map.of("schemaVersion", 1, "documentId", event.getDocumentId(),
                    "objectKey", event.getObjectKey(), "generation", event.getProcessingGeneration());
            sender.sendMessage(new ServiceBusMessage(AzureJson.write(body)).setContentType("application/json")
                    .setMessageId(event.getDocumentId() + ":" + event.getProcessingGeneration())
                    .setCorrelationId(event.getDocumentId().toString()));
        } catch (RuntimeException e) {
            throw new MessageBrokerUnavailableException("publish-document-uploaded", "Could not publish document job", e);
        }
    }
    @jakarta.annotation.PreDestroy
    @Override public void close() { sender.close(); }
}
