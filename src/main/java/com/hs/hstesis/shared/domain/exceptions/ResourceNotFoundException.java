package com.hs.hstesis.shared.domain.exceptions;

import java.util.List;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Long resourceId) {
        super(String.format("%s with id %d not found.", resourceName, resourceId));
    }
    public ResourceNotFoundException(String resourceName, Integer value) {
        super(String.format("%s %d not found.", resourceName, value));
    }
    public ResourceNotFoundException(String resourceName, List<?> ids) {
        super(String.format("%s with ids %s not found.", resourceName, ids.toString()));
    }
    public ResourceNotFoundException(String customMessage) {
        super(customMessage);
    }
}
