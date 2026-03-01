package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.learning.domain.model.commands.CreateCourseCommand;
import com.hs.hstesis.learning.domain.model.commands.EditCourseNameCommand;
import com.hs.hstesis.learning.domain.model.entities.Area;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    protected Course() {}

    public Course(CreateCourseCommand command, Area area) {
        this.name = command.name();
        this.area = area;
    }

    public void editName(EditCourseNameCommand command) {
        this.name = command.newName();
    }
}