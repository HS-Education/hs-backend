package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.exceptions.AcademicYearIsActiveException;
import com.hs.hstesis.learning.domain.model.valueobjects.AcademicYearStatus;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.AcademicYearRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
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
