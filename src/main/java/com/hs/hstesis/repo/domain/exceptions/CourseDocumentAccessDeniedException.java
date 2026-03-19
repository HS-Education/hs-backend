package com.hs.hstesis.repo.domain.exceptions;

import org.springframework.security.access.AccessDeniedException;

public class CourseDocumentAccessDeniedException extends AccessDeniedException {
    public CourseDocumentAccessDeniedException(Long courseId) {
        super(String.format("You do not have access to documents for course id: %d", courseId));
    }
}

