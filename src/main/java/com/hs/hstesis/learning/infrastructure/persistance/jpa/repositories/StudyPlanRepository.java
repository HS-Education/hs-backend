package com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    Optional<StudyPlan> findByEducationLevelAndGradeLevelAndCourseId(EducationLevel educationLevel, GradeLevel gradeLevel, Long courseId);
    List<StudyPlan> findAllByEducationLevelAndGradeLevel(EducationLevel educationLevel, GradeLevel gradeLevel);
    boolean existsByEducationLevelAndGradeLevelAndCourseId(EducationLevel educationLevel, GradeLevel gradeLevel, Long courseId);
}
