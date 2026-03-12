package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.learning.domain.model.commands.CreateSectionCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateSectionCommand;
import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"education_level", "grade_level", "name"}
        )
)
@Setter
@Getter
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EducationLevel educationLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GradeLevel gradeLevel;

    protected Section() {}

    public Section(CreateSectionCommand command) {
        this.name = command.name().toUpperCase().trim();
        this.educationLevel = command.educationLevel();
        this.gradeLevel = command.gradeLevel();
    }

    public void update(UpdateSectionCommand command) {
        if (command.name() != null) {
            changeName(command.name().toUpperCase().trim());
        }
    }

    private void changeName(String name) {
        this.name = name;
    }
}
