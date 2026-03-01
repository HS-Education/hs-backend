package com.hs.hstesis.learning.domain.exceptions;

public class AreaCoordinatorNotFoundException extends RuntimeException {
    public AreaCoordinatorNotFoundException(Long userId, Long areaId) {
        super(String.format("Coordinator not found for User %d in Area %d.", userId, areaId));
    }
}
