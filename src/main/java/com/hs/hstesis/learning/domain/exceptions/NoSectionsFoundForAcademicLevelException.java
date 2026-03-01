package com.hs.hstesis.learning.domain.exceptions;

public class NoSectionsFoundForAcademicLevelException extends RuntimeException {
    public NoSectionsFoundForAcademicLevelException(String levelName) {
        super(String.format("No sections were found for the level '%s'. " + "Please create at least one section before generating classrooms.",
                levelName));
    }
}