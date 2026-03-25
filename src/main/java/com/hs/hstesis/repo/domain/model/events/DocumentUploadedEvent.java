package com.hs.hstesis.repo.domain.model.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public final class DocumentUploadedEvent extends ApplicationEvent {

    private final Long documentId;
    private final String objectKey;

    public DocumentUploadedEvent(Object source, Long documentId, String objectKey) {
        super(source);
        this.documentId = documentId;
        this.objectKey = objectKey;
    }
}