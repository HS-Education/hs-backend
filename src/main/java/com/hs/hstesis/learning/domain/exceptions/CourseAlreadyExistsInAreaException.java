package com.hs.hstesis.learning.domain.exceptions;

public class CourseAlreadyExistsInAreaException extends RuntimeException {
    public CourseAlreadyExistsInAreaException(String name, String areaName) {
        super(String.format("A course with the name '%s' already exists in the area '%s'.", name, areaName));
    }
}
