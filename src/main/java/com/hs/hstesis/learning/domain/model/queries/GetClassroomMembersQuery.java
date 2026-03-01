package com.hs.hstesis.learning.domain.model.queries;

public record GetClassroomMembersQuery(Long classroomId) {
    public GetClassroomMembersQuery {
        if (classroomId == null) {
            throw new IllegalArgumentException("Classroom ID cannot be null");
        }
    }
}
