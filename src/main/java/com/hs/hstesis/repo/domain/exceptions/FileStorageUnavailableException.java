package com.hs.hstesis.repo.domain.exceptions;

public class FileStorageUnavailableException extends FileStorageException {
    public FileStorageUnavailableException(String operation, Throwable cause) {
        super("File storage is temporarily unavailable during operation: " + operation, cause);
    }
}
