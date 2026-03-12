package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.domain.model.commands.UpdateGradingPeriodCommand;
import com.hs.hstesis.learning.domain.model.valueobjects.GradingPeriodStatus;
import com.hs.hstesis.learning.domain.services.GradingPeriodCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.GradingPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class GradingPeriodCommandServiceImpl implements GradingPeriodCommandService {
    private final GradingPeriodRepository gradingPeriodRepository;

    public GradingPeriodCommandServiceImpl(GradingPeriodRepository gradingPeriodRepository) {
        this.gradingPeriodRepository = gradingPeriodRepository;
    }

    @Override
    @Transactional
    public Optional<GradingPeriod> handle(UpdateGradingPeriodCommand command) {
        var period = gradingPeriodRepository
                .findByIdAndAcademicYearId(command.gradingPeriodId(), command.academicYearId())
                .orElseThrow(() -> new GradingPeriodNotFoundException(command.gradingPeriodId()));

        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        if (period.getStatus() == GradingPeriodStatus.FINISHED) {
            throw new FinishedGradingPeriodModificationException(period.getBimester().getValue());
        }

        if (period.getStatus() == GradingPeriodStatus.ACTIVE) {
            if (!command.startDate().equals(period.getStartDate())) {
                throw new StartedGradingPeriodModificationException(period.getBimester().getValue());
            }
        }

        if (period.getStartDate() == null) {
            if (command.startDate().isBefore(today)) {
                throw new GradingPeriodInPastException();
            }
        }

        if (command.startDate().getYear() != currentYear || command.endDate().getYear() != currentYear) {
            throw new InvalidGradingPeriodYearException(currentYear);
        }

        long weeks = ChronoUnit.WEEKS.between(command.startDate(), command.endDate());
        if (weeks < 7) {
            throw new GradingPeriodDurationTooShortException(weeks);
        }

        List<GradingPeriod> otherPeriods = gradingPeriodRepository
                .findAllByAcademicYearId(period.getAcademicYear().getId())
                .stream()
                .filter(p -> !p.getId().equals(period.getId()))
                .filter(p -> p.getStartDate() != null && p.getEndDate() != null)
                .toList();

        for (GradingPeriod other : otherPeriods) {

            if (other == null || other.getStartDate() == null || other.getEndDate() == null) {
                continue;
            }

            int currentBimesterValue = period.getBimester().getValue();
            int otherBimesterValue = other.getBimester().getValue();

            if (otherBimesterValue < currentBimesterValue) {
                if (!command.startDate().isAfter(other.getEndDate())) {
                    throw new BimesterSequencePredecessorException(currentBimesterValue, otherBimesterValue, other.getEndDate());
                }
            }

            if (otherBimesterValue > currentBimesterValue) {
                if (!command.endDate().isBefore(other.getStartDate())) {
                    throw new BimesterSequenceSuccessorException(currentBimesterValue, otherBimesterValue, other.getStartDate());
                }
            }

            boolean isOverlapping =
                    !command.endDate().isBefore(other.getStartDate()) &&
                            !command.startDate().isAfter(other.getEndDate());

            if (isOverlapping) {
                throw new GradingPeriodOverlapException(
                        other.getBimester(),
                        other.getStartDate(),
                        other.getEndDate()
                );
            }
        }

        period.configureDates(command.startDate(), command.endDate());
        gradingPeriodRepository.save(period);
        return Optional.of(period);
    }
}