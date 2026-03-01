package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.learning.domain.model.entities.AcademicLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "study_plans",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"academic_level_id", "course_id"})
        }
)
@Getter
@Setter
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_level_id", nullable = false)
    private AcademicLevel academicLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    protected StudyPlan() {}

    public StudyPlan(AcademicLevel academicLevel, Course course) {
        this.academicLevel = academicLevel;
        this.course = course;
    }
}
