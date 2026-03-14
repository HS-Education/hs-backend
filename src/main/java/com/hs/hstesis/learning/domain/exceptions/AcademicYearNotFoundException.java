package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;

public class AcademicYearNotFoundException extends ResourceNotFoundException {
    public AcademicYearNotFoundException(Integer year) {
        super("Academic year", year);
    }
}
