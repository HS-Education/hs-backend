package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;

public class SectionNotFoundException extends ResourceNotFoundException {
    public SectionNotFoundException(Long id) {
        super("Section", id);
    }
}
