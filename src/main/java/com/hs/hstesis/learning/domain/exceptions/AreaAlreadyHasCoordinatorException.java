package com.hs.hstesis.learning.domain.exceptions;

public class AreaAlreadyHasCoordinatorException extends RuntimeException {
    public AreaAlreadyHasCoordinatorException(String areaName) {
        super(String.format("Area '%s' already has an assigned coordinator.", areaName));
    }
}
