package com.hs.hstesis.learning.domain.exceptions;

public class TeacherAlreadyAssignedException extends RuntimeException {
    public TeacherAlreadyAssignedException() {
        super("Teacher is already assigned to this classroom.");
    }
}
