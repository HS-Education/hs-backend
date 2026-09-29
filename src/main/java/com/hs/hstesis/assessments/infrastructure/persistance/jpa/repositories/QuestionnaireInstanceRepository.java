package com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireInstance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionnaireInstanceRepository extends JpaRepository<QuestionnaireInstance, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM QuestionnaireInstance i WHERE i.id = :id")
    Optional<QuestionnaireInstance> findByIdForUpdate(@Param("id") Long id);
    List<QuestionnaireInstance> findAllByQuestionnaireIdAndStudentId(Long questionnaireId, Long studentId);
    Optional<QuestionnaireInstance> findByQuestionnaireIdAndStudentIdIsNull(Long questionnaireId);
    List<QuestionnaireInstance> findAllByStudentId(Long studentId);
}
