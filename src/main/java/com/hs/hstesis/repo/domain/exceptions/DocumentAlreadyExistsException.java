package com.hs.hstesis.repo.domain.exceptions;

public class DocumentAlreadyExistsException extends RuntimeException {
    public DocumentAlreadyExistsException(String checksum) {
        super(String.format("Document with checksum %s already exists", checksum));
    }
}