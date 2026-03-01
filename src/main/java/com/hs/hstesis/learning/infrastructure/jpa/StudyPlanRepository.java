package com.hs.hstesis.learning.infrastructure.jpa;

import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findAllByAcademicLevelId(Long academicLevelId);
    boolean existsByAcademicLevelIdAndCourseId(Long academicLevelId, Long courseId);
}
