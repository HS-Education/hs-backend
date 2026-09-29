package com.hs.hstesis.repo.domain.exceptions;

public class InvalidPdfUploadException extends RuntimeException {
    public InvalidPdfUploadException() {
        super("Upload a valid, unencrypted PDF without active content (maximum 300 pages).");
    }
}
