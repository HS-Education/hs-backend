package com.hs.hstesis.repo.application.internal.eventhandlers;

import com.hs.hstesis.repo.domain.model.events.DocumentUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class DocumentUploadedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadedEventHandler.class);
    private final RabbitTemplate rabbitTemplate;

    public DocumentUploadedEventHandler(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(DocumentUploadedEvent event) {
        log.info("Document saved in DB. Sending to AI Worker via RabbitMQ. Document ID: {}", event.getDocumentId());

        rabbitTemplate.convertAndSend("", "document_processing_queue", event);

        System.out.println("DocumentUploadedEventHandler executed");
    }
}
