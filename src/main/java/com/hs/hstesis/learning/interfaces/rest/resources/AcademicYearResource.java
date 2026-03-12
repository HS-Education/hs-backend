package com.hs.hstesis.learning.interfaces.rest.resources;

import com.hs.hstesis.learning.domain.model.valueobjects.AcademicYearStatus;

public record AcademicYearResource(Long id,
                                   Integer year,
                                   AcademicYearStatus status) {
}
