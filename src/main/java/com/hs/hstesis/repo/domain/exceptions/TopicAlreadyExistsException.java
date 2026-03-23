package com.hs.hstesis.repo.domain.exceptions;

public class TopicAlreadyExistsException extends RuntimeException {
    public TopicAlreadyExistsException(String topicName) {
        super(String.format("Topic %s already exists", topicName));
    }
}
