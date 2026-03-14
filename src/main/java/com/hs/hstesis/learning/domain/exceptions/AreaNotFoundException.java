package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException;

public class AreaNotFoundException extends ResourceNotFoundException {
    public AreaNotFoundException(Long id) {
        super("Area", id);
    }
}
