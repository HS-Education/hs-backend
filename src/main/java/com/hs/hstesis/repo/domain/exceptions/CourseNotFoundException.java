package com.hs.hstesis.repo.domain.exceptions;

import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;

public class CourseNotFoundException extends ResourceNotFoundException {
    public CourseNotFoundException(Long courseId) {
        super("Course", courseId);
    }
}
