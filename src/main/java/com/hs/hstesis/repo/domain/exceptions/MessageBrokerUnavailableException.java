package com.hs.hstesis.repo.domain.exceptions;

public class MessageBrokerUnavailableException extends RuntimeException {
    public MessageBrokerUnavailableException(String operation, String message) {
        super("[" + operation + "] " + message);
    }

    public MessageBrokerUnavailableException(String operation, String message, Throwable cause) {
        super("[" + operation + "] " + message, cause);
    }
}

