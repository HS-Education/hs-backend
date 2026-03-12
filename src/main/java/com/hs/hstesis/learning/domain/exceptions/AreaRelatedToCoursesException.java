package com.hs.hstesis.learning.domain.exceptions;

public class AreaRelatedToCoursesException extends RuntimeException {
    public AreaRelatedToCoursesException(String name) {
        super(String.format("Area '%s' cannot be deleted because it has associated courses.", name));
    }
}
