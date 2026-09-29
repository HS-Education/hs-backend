package com.hs.hstesis.repo.application.internal.eventhandlers;

import com.hs.hstesis.repo.application.internal.outboundservices.messaging.DocumentProcessingPublisher;
import com.hs.hstesis.repo.application.internal.commandservices.DocumentStatusTransitionService;
import com.hs.hstesis.repo.domain.model.events.DocumentUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class DocumentUploadedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadedEventHandler.class);
    private final DocumentProcessingPublisher documentProcessingPublisher;
    private final DocumentStatusTransitionService statusTransitions;

    public DocumentUploadedEventHandler(
            DocumentProcessingPublisher documentProcessingPublisher,
            DocumentStatusTransitionService statusTransitions) {
        this.documentProcessingPublisher = documentProcessingPublisher;
        this.statusTransitions = statusTransitions;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(DocumentUploadedEvent event) {
        // Commit PROCESSING before publishing: a fast worker may finish immediately.
        statusTransitions.markProcessing(event.getDocumentId());
        try {
            documentProcessingPublisher.publish(event);
        } catch (RuntimeException exception) {
            statusTransitions.markFailed(event.getDocumentId());
            log.error("Could not queue documentId={} for processing", event.getDocumentId());
        }
    }
}
