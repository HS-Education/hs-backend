package com.hs.hstesis.repo.application.internal.eventhandlers;

import com.hs.hstesis.repo.application.internal.outboundservices.messaging.DocumentProcessingPublisher;
import com.hs.hstesis.repo.domain.exceptions.DocumentNotFoundException;
import com.hs.hstesis.repo.domain.model.events.DocumentUploadedEvent;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class DocumentUploadedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadedEventHandler.class);
    private final DocumentProcessingPublisher documentProcessingPublisher;
    private final DocumentRepository documentRepository;

    public DocumentUploadedEventHandler(
            DocumentProcessingPublisher documentProcessingPublisher,
            DocumentRepository documentRepository) {
        this.documentProcessingPublisher = documentProcessingPublisher;
        this.documentRepository = documentRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(DocumentUploadedEvent event) {
        log.info("Document saved in DB. Sending to AI Worker via RabbitMQ. Document ID: {}", event.getDocumentId());

        documentProcessingPublisher.publish(event);

        var document = documentRepository.findById(event.getDocumentId())
                .orElseThrow(() -> new DocumentNotFoundException(event.getDocumentId()));

        document.markAsProcessing();
        documentRepository.save(document);

        log.info("Document {} marked as PROCESSING", event.getDocumentId());
    }
}