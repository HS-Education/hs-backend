package com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionnaireSubmissionRepository extends JpaRepository<QuestionnaireSubmission, Long> {
    Optional<QuestionnaireSubmission> findFirstByQuestionnaireInstanceIdAndStudentIdOrderBySubmittedAtDesc(Long questionnaireInstanceId, Long studentId);

    List<QuestionnaireSubmission> findByStudentId(Long studentId);

    @EntityGraph(attributePaths = {"questionnaireInstance.questionnaire", "answers.question"})
    List<QuestionnaireSubmission> findByQuestionnaireInstance_Questionnaire_CourseIdAndStudentIdIn(
            Long courseId, java.util.Collection<Long> studentIds);
}
