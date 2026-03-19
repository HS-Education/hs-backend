package com.hs.hstesis.learning.domain.model.queries;

public record ExistsCourseForCoordinatorQuery(Long courseId, Long coordinatorId) {
    public ExistsCourseForCoordinatorQuery {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
        if (coordinatorId == null || coordinatorId <= 0) {
            throw new IllegalArgumentException("Coordinator id cannot be null or negative.");
        }
    }
}
