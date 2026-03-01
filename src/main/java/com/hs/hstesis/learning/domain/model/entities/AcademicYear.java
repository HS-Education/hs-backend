package com.hs.hstesis.learning.domain.model.entities;

import com.hs.hstesis.learning.domain.model.commands.CreateAcademicYearCommand;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Boolean isActive;

    protected AcademicYear() {}

    public AcademicYear(CreateAcademicYearCommand command){
        this.year = command.year();
        this.isActive = false;
    }
}
