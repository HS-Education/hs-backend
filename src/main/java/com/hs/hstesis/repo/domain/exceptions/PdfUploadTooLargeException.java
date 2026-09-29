package com.hs.hstesis.repo.domain.exceptions;

public class PdfUploadTooLargeException extends RuntimeException {
    public PdfUploadTooLargeException() {
        super("PDF upload exceeds the 50 MiB limit.");
    }
}
