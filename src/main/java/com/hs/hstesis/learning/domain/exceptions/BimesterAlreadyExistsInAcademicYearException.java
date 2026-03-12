package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.learning.domain.model.valueobjects.Bimester;

public class BimesterAlreadyExistsInAcademicYearException extends RuntimeException {
    public BimesterAlreadyExistsInAcademicYearException(Bimester bimester, Integer year) {
        super(String.format("Bimester '%s' already exists in academic year '%d'.", bimester, year));
    }
}
