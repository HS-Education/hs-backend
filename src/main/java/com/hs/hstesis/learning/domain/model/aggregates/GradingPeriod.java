package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import com.hs.hstesis.learning.domain.model.valueobjects.Bimester;
import com.hs.hstesis.learning.domain.model.valueobjects.GradingPeriodStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "grading_periods",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"academic_year_id", "bimester"}
                )
        }
)
@Getter
@Setter
public class GradingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Bimester bimester;

    private LocalDate startDate;

    private LocalDate endDate;

    protected GradingPeriod () {}

    public GradingPeriod(Bimester bimester, AcademicYear academicYear) {
        this.bimester = bimester;
        this.academicYear = academicYear;
    }

    public void configureDates(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public GradingPeriodStatus getStatus() {

        LocalDate today = LocalDate.now();

        if (startDate == null || endDate == null) {
            return GradingPeriodStatus.PLANNED;
        }

        if (today.isBefore(startDate)) {
            return GradingPeriodStatus.PLANNED;
        }

        if (!today.isAfter(endDate)) {
            return GradingPeriodStatus.ACTIVE;
        }

        return GradingPeriodStatus.FINISHED;
    }
}
