package com.hs.hstesis.repo.domain.exceptions;

public class TopicDoesNotBelongToCourseException extends RuntimeException {
    public TopicDoesNotBelongToCourseException(Long topicId, Long courseId) {
        super(String.format("Topic %d does not belong to course %d", topicId, courseId));
    }
}

