package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.commands.ActivateAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.commands.CloseAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.commands.CreateAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import com.hs.hstesis.learning.domain.services.AcademicYearCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AcademicYearRepository;
import com.hs.hstesis.learning.infrastructure.jpa.GradingPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AcademicYearCommandServiceImpl implements AcademicYearCommandService {
    private final AcademicYearRepository academicYearRepository;
    private final GradingPeriodRepository gradingPeriodRepository;

    public AcademicYearCommandServiceImpl(AcademicYearRepository academicYearRepository, GradingPeriodRepository gradingPeriodRepository) {
        this.academicYearRepository = academicYearRepository;
        this.gradingPeriodRepository = gradingPeriodRepository;
    }

    @Override
    public void handle(CreateAcademicYearCommand command){
        if(academicYearRepository.existsByYear(command.year())){
            throw new AcademicYearAlreadyExistsException(command.year());
        }
        var academicYear = new AcademicYear(command);
        academicYearRepository.save(academicYear);
    }

    @Override
    public void handle(DeleteAcademicYearCommand command){
        if(!academicYearRepository.existsById(command.id())) {
            throw new AcademicYearNotFoundException(command.id());
        }
        academicYearRepository.deleteById(command.id());
    }

    @Override
    @Transactional
    public void handle(ActivateAcademicYearCommand command) {
        var yearToActivate = academicYearRepository.findById(command.id())
                .orElseThrow(() -> new AcademicYearNotFoundException(command.id()));

        long periodsCount = gradingPeriodRepository.countByAcademicYearId(yearToActivate.getId());

        if (periodsCount < 4) {
            throw new IncompleteAcademicYearException(yearToActivate.getYear(), periodsCount);
        }

        academicYearRepository.deactivateAllYears();

        yearToActivate.setIsActive(true);
        academicYearRepository.save(yearToActivate);
    }

    @Override
    @Transactional
    public void handle(CloseAcademicYearCommand command) {
        var academicYear = academicYearRepository.findById(command.id())
                .orElseThrow(() -> new AcademicYearNotFoundException(command.id()));

        var lastPeriod = gradingPeriodRepository.findByBimesterAndAcademicYearId(4, academicYear.getId())
                .orElseThrow(() -> new GradingPeriodNotFoundException(4, academicYear.getYear()));

        if (LocalDate.now().isBefore(lastPeriod.getEndDate())) {
            throw new AcademicYearCannotBeClosedException(academicYear.getYear(), lastPeriod.getEndDate());
        }

        academicYear.setIsActive(false);
        academicYearRepository.save(academicYear);
    }
}
