package com.hs.hstesis.learning.domain.exceptions;

public class CourseNameAlreadyExistsInAreaException extends RuntimeException {
    public CourseNameAlreadyExistsInAreaException(String name, String areaName) {
        super(String.format("Course name '%s' already exists in area '%s'.", name, areaName));
    }
}
