package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.learning.domain.model.commands.CreateGradingPeriodCommand;
import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class GradingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AcademicYear academicYear;

    private Integer bimester;

    private LocalDate startDate;
    private LocalDate endDate;

    private Boolean isActive;

    protected GradingPeriod () {}

    public GradingPeriod(CreateGradingPeriodCommand command, AcademicYear academicYear) {
        this.academicYear = academicYear;
        this.bimester = command.bimester();
        this.startDate = command.startDate();
        this.endDate = command.endDate();
        this.updateStatusBasedOnDate(LocalDate.now());
    }

    public void updateStatusBasedOnDate(LocalDate today) {
        this.isActive = !today.isBefore(this.startDate) && !today.isAfter(this.endDate);
    }
}
