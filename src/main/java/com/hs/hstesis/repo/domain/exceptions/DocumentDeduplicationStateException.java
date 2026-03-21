package com.hs.hstesis.repo.domain.exceptions;

public class DocumentDeduplicationStateException extends RuntimeException {
    public DocumentDeduplicationStateException(String checksum, Throwable cause) {
        super(String.format("Deduplication inconsistency: save failed but no document found for checksum=%s", checksum), cause);
    }
}
