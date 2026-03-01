package com.hs.hstesis.learning.domain.model.queries;

public record GetAllClassroomsByTeacherIdQuery(Long teacherId) {
    public GetAllClassroomsByTeacherIdQuery {
        if (teacherId == null) {
            throw new IllegalArgumentException("TeacherId cannot be null");
        }
    }
}
