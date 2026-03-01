package com.hs.hstesis.learning.domain.exceptions;

public class EnrollmentNotFoundException extends RuntimeException {
    public EnrollmentNotFoundException(Long userId, Long classroomId) {
        super(String.format("Enrollment not found for user ID %d in classroom ID %d", userId, classroomId));
    }
}
