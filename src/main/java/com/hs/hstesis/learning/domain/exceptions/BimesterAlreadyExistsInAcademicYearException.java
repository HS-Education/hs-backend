package com.hs.hstesis.learning.domain.exceptions;

public class BimesterAlreadyExistsInAcademicYearException extends RuntimeException {
    public BimesterAlreadyExistsInAcademicYearException(Integer bimester, Long academicYearId) {
        super(String.format("Bimester '%d' already exists in academic year '%d'.", bimester, academicYearId));
    }
}
