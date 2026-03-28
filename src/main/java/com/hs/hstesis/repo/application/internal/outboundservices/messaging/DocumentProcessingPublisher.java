package com.hs.hstesis.repo.application.internal.outboundservices.messaging;

import com.hs.hstesis.repo.domain.model.events.DocumentUploadedEvent;

public interface DocumentProcessingPublisher {
    void publish(DocumentUploadedEvent event);
}
