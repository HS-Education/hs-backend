package com.hs.hstesis.learning.domain.model.queries;

public record GetAllClassroomsByStudentIdQuery(Long studentId) {
    public GetAllClassroomsByStudentIdQuery {
        if (studentId == null) {
            throw new IllegalArgumentException("StudentId cannot be null");
        }
    }
}
