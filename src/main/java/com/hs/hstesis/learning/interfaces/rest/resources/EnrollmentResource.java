package com.hs.hstesis.learning.interfaces.rest.resources;

public record EnrollmentResource(
        Long id,
        Long userId,
        String userName,
        Long classroomId,
        String roleInClassroom
) {
}
