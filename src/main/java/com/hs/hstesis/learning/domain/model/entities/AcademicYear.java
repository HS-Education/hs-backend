package com.hs.hstesis.learning.domain.model.entities;

import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.domain.model.valueobjects.AcademicYearStatus;
import com.hs.hstesis.learning.domain.model.valueobjects.GradingPeriodStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @OneToMany(mappedBy = "academicYear", fetch = FetchType.EAGER)
    private List<GradingPeriod> periods = new ArrayList<>();

    protected AcademicYear() {}

    public AcademicYear(int year){
        this.year = year;
    }

    public AcademicYearStatus getStatus() {

        if (periods.isEmpty()) {
            return AcademicYearStatus.PLANNED;
        }

        boolean isPlanningComplete = periods.stream()
                .allMatch(p -> p.getStartDate() != null && p.getEndDate() != null);
        if (!isPlanningComplete) {
            return AcademicYearStatus.PLANNED;
        }

        boolean anyActive = periods.stream()
                .anyMatch(p -> p.getStatus() == GradingPeriodStatus.ACTIVE);
        if (anyActive) {
            return AcademicYearStatus.ACTIVE;
        }

        boolean allFinished = periods.stream()
                .allMatch(p -> p.getStatus() == GradingPeriodStatus.FINISHED);
        if (allFinished) {
            return AcademicYearStatus.CLOSED;
        }


        return AcademicYearStatus.PLANNED;
    }
}
