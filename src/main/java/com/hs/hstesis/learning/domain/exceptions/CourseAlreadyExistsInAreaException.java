package com.hs.hstesis.learning.domain.exceptions;

public class CourseAlreadyExistsInAreaException extends RuntimeException {
    public CourseAlreadyExistsInAreaException(String name, String areaName) {
        super(String.format("Course name '%s' already exists in area '%s'.", name, areaName));
    }
}
