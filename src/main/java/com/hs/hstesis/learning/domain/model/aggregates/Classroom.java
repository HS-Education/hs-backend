package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import com.hs.hstesis.learning.domain.model.valueobjects.AcademicYearStatus;
import com.hs.hstesis.learning.domain.model.valueobjects.ClassroomStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "classrooms",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"course_id", "section_id", "academic_year_id"}
        )
)
@Getter
@Setter
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    protected Classroom() {}

    public Classroom(Course course, Section section, AcademicYear academicYear) {
        this.course = course;
        this.section = section;
        this.academicYear = academicYear;
    }

    public ClassroomStatus getStatus() {
        if (academicYear.getStatus() == AcademicYearStatus.ACTIVE) {
            return ClassroomStatus.ACTIVE;
        }

        return ClassroomStatus.INACTIVE;
    }
}
