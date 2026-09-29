package com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.assessments.domain.model.entities.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findAllByQuestionnaireInstanceId(Long questionnaireInstanceId);

    @Query("""
            select q from Question q
            where q.topicId = :topicId
              and q.questionnaireInstance.studentId is null
              and q.isRemedial = false
            """)
    List<Question> findBaseQuestionsByTopicId(@Param("topicId") Long topicId);
}
