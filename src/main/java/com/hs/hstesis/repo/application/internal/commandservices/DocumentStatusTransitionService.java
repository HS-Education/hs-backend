package com.hs.hstesis.repo.application.internal.commandservices;

import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentStatusTransitionService {
    private final DocumentRepository documents;

    public DocumentStatusTransitionService(DocumentRepository documents) {
        this.documents = documents;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(Long documentId) {
        documents.findById(documentId).ifPresent(document -> {
            document.markAsProcessing();
            documents.save(document);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long documentId) {
        documents.findById(documentId).ifPresent(document -> {
            document.markAsFailed();
            documents.save(document);
        });
    }
}
