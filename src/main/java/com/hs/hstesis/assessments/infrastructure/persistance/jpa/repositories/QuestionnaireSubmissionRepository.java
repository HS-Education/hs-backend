package com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionnaireSubmissionRepository extends JpaRepository<QuestionnaireSubmission, Long> {
    Optional<QuestionnaireSubmission> findFirstByQuestionnaireInstanceIdAndStudentIdOrderBySubmittedAtDesc(Long questionnaireInstanceId, Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"answers.question"})
    @Query("SELECT s FROM QuestionnaireSubmission s WHERE s.questionnaireInstance.id = :instanceId AND s.studentId = :studentId")
    Optional<QuestionnaireSubmission> findForUpdateByInstanceAndStudent(@Param("instanceId") Long instanceId,
                                                                         @Param("studentId") Long studentId);

    @EntityGraph(attributePaths = {"answers.question"})
    @Query("SELECT s FROM QuestionnaireSubmission s WHERE s.id = :id")
    Optional<QuestionnaireSubmission> findByIdWithAnswers(@Param("id") Long id);

    List<QuestionnaireSubmission> findByStudentId(Long studentId);

    @EntityGraph(attributePaths = {"questionnaireInstance.questionnaire", "answers.question"})
    List<QuestionnaireSubmission> findByQuestionnaireInstance_Questionnaire_CourseIdAndStudentIdIn(
            Long courseId, java.util.Collection<Long> studentIds);
}
