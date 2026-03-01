package com.hs.hstesis.learning.domain.exceptions;

public class AreaAlreadyHasCoordinatorException extends RuntimeException {
    public AreaAlreadyHasCoordinatorException(Long areaId) {
        super(String.format("Area with id %d already has a coordinator.", areaId));
    }
}
