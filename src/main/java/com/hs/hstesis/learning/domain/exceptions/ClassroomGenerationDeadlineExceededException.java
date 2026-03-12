package com.hs.hstesis.learning.domain.exceptions;

public class ClassroomGenerationDeadlineExceededException extends RuntimeException {
    public ClassroomGenerationDeadlineExceededException() {
        super("Classrooms can only be generated during the PLANNED status of the academic year.");
    }
}
