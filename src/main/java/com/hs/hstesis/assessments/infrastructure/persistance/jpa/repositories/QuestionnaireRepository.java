package com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.assessments.domain.model.aggregates.Questionnaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {
    boolean existsByCourseIdAndGradingPeriodIdAndWeekNumber(Long courseId, Long gradingPeriodId, Integer weekNumber);
    java.util.List<Questionnaire> findAllByCourseIdAndGradingPeriodId(Long courseId, Long gradingPeriodId);
    java.util.List<Questionnaire> findAllByCourseId(Long courseId);
}
