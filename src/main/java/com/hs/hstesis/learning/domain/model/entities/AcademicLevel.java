package com.hs.hstesis.learning.domain.model.entities;

import com.hs.hstesis.learning.domain.model.commands.CreateAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.EditAcademicLevelNameCommand;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class AcademicLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    protected AcademicLevel() {};

    public AcademicLevel(CreateAcademicLevelCommand command){
        this.name = command.name();
    }

    public void editName(EditAcademicLevelNameCommand command){
        this.name = command.newName();
    }
}