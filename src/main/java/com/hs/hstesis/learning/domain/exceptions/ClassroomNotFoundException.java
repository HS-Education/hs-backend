package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;

import java.util.List;

public class ClassroomNotFoundException extends ResourceNotFoundException {
    public ClassroomNotFoundException(Long id) {
        super("Classroom", id);
    }

    public ClassroomNotFoundException(List<Long> ids) {
        super("Classrooms", ids);
    }
}
