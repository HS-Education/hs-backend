package com.hs.hstesis.learning.domain.model.queries;

public record ExistsEnrollmentByUserIdAndClassroomIdQuery(Long userId, Long classroomId) {
    public ExistsEnrollmentByUserIdAndClassroomIdQuery {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id cannot be null or negative.");
        }
        if (classroomId == null || classroomId <= 0) {
            throw new IllegalArgumentException("Classroom id cannot be null or negative.");
        }
    }
}
