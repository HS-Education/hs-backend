package com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.publishers;

import com.hs.hstesis.repo.application.internal.outboundservices.messaging.DocumentProcessingPublisher;
import com.hs.hstesis.repo.domain.exceptions.MessageBrokerUnavailableException;
import com.hs.hstesis.repo.domain.model.events.DocumentUploadedEvent;
import com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos.DocumentProcessingMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DocumentProcessingPublisherRabbitAdapter implements DocumentProcessingPublisher {

    private final RabbitTemplate rabbitTemplate;

    public DocumentProcessingPublisherRabbitAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(DocumentUploadedEvent event) {
        CorrelationData cd = new CorrelationData(event.getDocumentId().toString());

        var payload = new DocumentProcessingMessage(event.getDocumentId(), event.getObjectKey());

        rabbitTemplate.convertAndSend(
                "",
                "document_processing_queue",
                payload,
                cd
        );

        try {
            CorrelationData.Confirm confirm = cd.getFuture().get(5, TimeUnit.SECONDS);

            if (!confirm.ack()) {
                throw new MessageBrokerUnavailableException(
                        "publish-document-uploaded",
                        "RabbitMQ NACK publishing documentId=" + event.getDocumentId()
                );
            }

            if (cd.getReturned() != null) {
                throw new MessageBrokerUnavailableException(
                        "publish-document-uploaded",
                        "Message returned (unroutable) for documentId=" + event.getDocumentId()
                );
            }

        } catch (MessageBrokerUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageBrokerUnavailableException(
                    "publish-document-uploaded",
                    "Error publishing documentId=" + event.getDocumentId(),
                    e
            );
        }
    }
}