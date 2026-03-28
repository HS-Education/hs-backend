package com.hs.hstesis.repo.domain.exceptions;

import com.hs.hstesis.repo.domain.model.valueobjects.DocumentStatus;

public class InvalidDocumentStatusTransitionException extends RuntimeException {
    public InvalidDocumentStatusTransitionException(DocumentStatus from, DocumentStatus to) {
        super(String.format("Invalid document status transition from %s to %s", from, to));
    }
}
