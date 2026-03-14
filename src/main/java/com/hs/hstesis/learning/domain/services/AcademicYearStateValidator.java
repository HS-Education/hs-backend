package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.exceptions.AcademicYearIsActiveException;
import com.hs.hstesis.learning.domain.exceptions.AcademicYearNotFoundException;
import com.hs.hstesis.learning.domain.model.valueobjects.AcademicYearStatus;
import com.hs.hstesis.learning.infrastructure.jpa.AcademicYearRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AcademicYearStateValidator {
    private final AcademicYearRepository academicYearRepository;

    public AcademicYearStateValidator(AcademicYearRepository academicYearRepository) {
        this.academicYearRepository = academicYearRepository;
    }

    public void validateCurrentYearIsNotActive() {
        int currentYearValue = LocalDate.now().getYear();

        academicYearRepository.findByYear(currentYearValue)
                .ifPresent(academicYear -> {
                    if (academicYear.getStatus() == AcademicYearStatus.ACTIVE) {
                        throw new AcademicYearIsActiveException();
                    }
                });
    }
}
