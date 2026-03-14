package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;

public class EnrollmentNotFoundException extends ResourceNotFoundException {
    public EnrollmentNotFoundException() { super("Enrollment not found."); }
}
