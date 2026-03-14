package com.hs.hstesis.learning.domain.model.queries;

public record GetClassroomMembersQuery(Long classroomId) {
    public GetClassroomMembersQuery {
        if (classroomId == null ||  classroomId <= 0) {
            throw new IllegalArgumentException("Classroom id cannot be null or negative.");
        }
    }
}
