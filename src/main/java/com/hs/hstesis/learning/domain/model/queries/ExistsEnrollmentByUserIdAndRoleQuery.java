package com.hs.hstesis.learning.domain.model.queries;

public record ExistsEnrollmentByUserIdAndRoleQuery(Long userId, String roleInClassroom) {
    public ExistsEnrollmentByUserIdAndRoleQuery {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id cannot be null or negative.");
        }
        if (roleInClassroom == null || roleInClassroom.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or blank");
        }
    }
}
