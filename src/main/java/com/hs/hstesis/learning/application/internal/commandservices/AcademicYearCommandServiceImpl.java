package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.domain.model.commands.GenerateAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import com.hs.hstesis.learning.domain.model.valueobjects.Bimester;
import com.hs.hstesis.learning.domain.services.AcademicYearCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AcademicYearRepository;
import com.hs.hstesis.learning.infrastructure.jpa.GradingPeriodRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AcademicYearCommandServiceImpl implements AcademicYearCommandService {
    private final AcademicYearRepository academicYearRepository;
    private final GradingPeriodRepository gradingPeriodRepository;

    public AcademicYearCommandServiceImpl(AcademicYearRepository academicYearRepository,
                                          GradingPeriodRepository gradingPeriodRepository) {
        this.academicYearRepository = academicYearRepository;
        this.gradingPeriodRepository = gradingPeriodRepository;
    }

    @Override
    public Long handle(GenerateAcademicYearCommand command) {
        int currentYear = LocalDate.now().getYear();

        if (academicYearRepository.existsByYear(currentYear)) {
            throw new AcademicYearAlreadyExistsException(currentYear);
        }

        var academicYear = new AcademicYear(currentYear);
        academicYearRepository.save(academicYear);

        for (Bimester bimester : Bimester.values()) {
            var period = new GradingPeriod(bimester, academicYear);
            gradingPeriodRepository.save(period);
        }

        return academicYear.getId();
    }
}
