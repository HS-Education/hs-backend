package com.hs.hstesis.repo.domain.exceptions;

public class DocumentWithoutTargetsException extends RuntimeException {
    public DocumentWithoutTargetsException(Long documentId) {
        super(String.format("Document with id %s does not have targets", documentId));
    }
}
