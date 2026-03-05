package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findAllByAcademicLevelId(Long academicLevelId);
    Optional<StudyPlan> findByAcademicLevelIdAndCourseId(Long academicLevelId, Long courseId);
    boolean existsByAcademicLevelIdAndCourseId(Long academicLevelId, Long courseId);
}
