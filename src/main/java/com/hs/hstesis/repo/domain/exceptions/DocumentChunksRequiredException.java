package com.hs.hstesis.repo.domain.exceptions;

public class DocumentChunksRequiredException extends RuntimeException {
    public DocumentChunksRequiredException(Long documentId) {
        super(String.format("Document with id %d must have at least one chunk to be processed.", documentId));
    }
}
