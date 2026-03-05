package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.domain.model.commands.CreateGradingPeriodCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteGradingPeriodCommand;
import com.hs.hstesis.learning.domain.services.GradingPeriodCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AcademicYearRepository;
import com.hs.hstesis.learning.infrastructure.jpa.ClassroomRepository;
import com.hs.hstesis.learning.infrastructure.jpa.GradingPeriodRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class GradingPeriodCommandServiceImpl implements GradingPeriodCommandService {
    private final GradingPeriodRepository gradingPeriodRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassroomRepository classroomRepository;

    public GradingPeriodCommandServiceImpl(GradingPeriodRepository gradingPeriodRepository,
                                           AcademicYearRepository academicYearRepository,
                                           ClassroomRepository classroomRepository) {
        this.gradingPeriodRepository = gradingPeriodRepository;
        this.academicYearRepository = academicYearRepository;
        this.classroomRepository = classroomRepository;
    }

    @Override
    public Long handle(CreateGradingPeriodCommand command) {
        if (command.startDate().isBefore(LocalDate.now())) {
            throw new DateInPastException(command.startDate());
        }

        var academicYear = academicYearRepository.findById(command.academicYearId())
                .orElseThrow(() -> new AcademicYearNotFoundException(command.academicYearId()));

        int yearValue = academicYear.getYear();
        if (command.startDate().getYear() != yearValue || command.endDate().getYear() != yearValue) {
            throw new DateYearMismatchException(command.startDate(), yearValue);
        }

        if (gradingPeriodRepository.existsByBimesterAndAcademicYearId(command.bimester(), command.academicYearId())){
            throw new BimesterAlreadyExistsInAcademicYearException(command.bimester(), command.academicYearId());
        }

        List<GradingPeriod> existingPeriods = gradingPeriodRepository.findAllByAcademicYearId(command.academicYearId());
        for (GradingPeriod period : existingPeriods) {
            boolean isOverlapping = command.startDate().isBefore(period.getEndDate().plusDays(1)) &&
                    command.endDate().isAfter(period.getStartDate().minusDays(1));

            if (isOverlapping) {
                throw new GradingPeriodOverlapException(period.getBimester(), period.getStartDate(), period.getEndDate());
            }
        }

        var gradingPeriod = new GradingPeriod(command, academicYear);
        gradingPeriodRepository.save(gradingPeriod);
        return gradingPeriod.getId();
    }

    @Override
    public void handle(DeleteGradingPeriodCommand command){
        var gradingPeriod = gradingPeriodRepository.findById(command.id())
                .orElseThrow(() -> new GradingPeriodNotFoundException(command.id()));
        if(!gradingPeriod.getStartDate().isAfter(LocalDate.now())){
            String yearName = gradingPeriod.getAcademicYear().getYear().toString();
            throw new InvalidGradingPeriodDeleteException(gradingPeriod.getBimester(), yearName);
        }
        gradingPeriodRepository.delete(gradingPeriod);
    }

    @Override
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void refreshPeriodsStatus() {
        LocalDate today = LocalDate.now();

        var activeYear = academicYearRepository.findByIsActiveTrue()
                .orElse(null);

        if (activeYear != null) {

            if (today.getYear() > activeYear.getYear()) {
                activeYear.setIsActive(false);
                academicYearRepository.save(activeYear);
                return;
            }

            List<GradingPeriod> periods = gradingPeriodRepository.findAllByAcademicYearId(activeYear.getId());
            for (GradingPeriod period : periods) {
                boolean wasActive = period.getIsActive();
                period.updateStatusBasedOnDate(today);

                if (!wasActive && period.getIsActive() && period.getBimester() == 1) {
                    classroomRepository.activateAllByAcademicYearId(activeYear.getId());
                }

                if (period.getBimester() == 4 && today.isEqual(period.getEndDate().plusDays(1))) {
                    classroomRepository.updateIsActiveByAcademicYearId(activeYear.getId(), false);
                }
            }
        }
    }
}
