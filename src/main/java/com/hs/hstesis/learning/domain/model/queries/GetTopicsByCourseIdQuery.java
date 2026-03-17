package com.hs.hstesis.learning.domain.model.queries;

public record GetTopicsByCourseIdQuery(Long courseId) {
    public GetTopicsByCourseIdQuery {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
    }
}
