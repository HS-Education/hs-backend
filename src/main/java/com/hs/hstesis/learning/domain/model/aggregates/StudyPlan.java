package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"education_level","grade_level","course_id"}
        )
)
@Getter
@Setter
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EducationLevel educationLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GradeLevel gradeLevel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    protected StudyPlan() {}

    public StudyPlan(EducationLevel educationLevel, GradeLevel gradeLevel, Course course) {
        this.educationLevel = educationLevel;
        this.gradeLevel = gradeLevel;
        this.course = course;
    }
}
