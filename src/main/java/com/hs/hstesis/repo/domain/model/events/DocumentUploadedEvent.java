package com.hs.hstesis.repo.domain.model.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public final class DocumentUploadedEvent extends ApplicationEvent {

    private final Long documentId;
    private final String objectKey;
    private final int processingGeneration;

    public DocumentUploadedEvent(Object source, Long documentId, String objectKey) {
        this(source, documentId, objectKey, 1);
    }

    public DocumentUploadedEvent(Object source, Long documentId, String objectKey, int processingGeneration) {
        super(source);
        this.documentId = documentId;
        this.objectKey = objectKey;
        this.processingGeneration = processingGeneration;
    }
}
