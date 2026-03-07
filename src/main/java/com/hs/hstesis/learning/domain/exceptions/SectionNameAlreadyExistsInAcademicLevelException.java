package com.hs.hstesis.learning.domain.exceptions;

public class SectionNameAlreadyExistsInAcademicLevelException extends RuntimeException {
    public SectionNameAlreadyExistsInAcademicLevelException(String name, String academicLevelName) {
        super(String.format("Section name '%s' already exists in academic level '%s'.", name, academicLevelName));
    }
}
