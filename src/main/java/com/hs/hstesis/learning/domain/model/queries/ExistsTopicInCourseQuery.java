package com.hs.hstesis.learning.domain.model.queries;

public record ExistsTopicInCourseQuery(Long topicId, Long courseId) {
    public ExistsTopicInCourseQuery {
        if (topicId == null || topicId <= 0) {
            throw new IllegalArgumentException("Topic id cannot be null or negative.");
        }
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
    }
}
