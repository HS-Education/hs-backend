package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.learning.domain.model.commands.CreateSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.EditSectionNameCommand;
import com.hs.hstesis.learning.domain.model.entities.AcademicLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "academic_level_id", nullable = false)
    private AcademicLevel academicLevel;

    protected Section() {}

    public Section(CreateSectionCommand command, AcademicLevel academicLevel){
        this.name = command.name();
        this.academicLevel = academicLevel;
    }

    public void editName(EditSectionNameCommand command) {
        this.name = command.newName();
    }
}
