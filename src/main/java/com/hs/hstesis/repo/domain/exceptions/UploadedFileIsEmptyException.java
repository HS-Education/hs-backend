package com.hs.hstesis.repo.domain.exceptions;

public class UploadedFileIsEmptyException extends RuntimeException {
    public UploadedFileIsEmptyException() {
        super("The uploaded file is empty.");
    }
}
