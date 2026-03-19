package com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
     SELECT DISTINCT d
     FROM Document d
     JOIN d.targets t
     WHERE t.id.courseId = :courseId
       AND t.id.educationLevel = :educationLevel
       AND t.id.gradeLevel = :gradeLevel
       AND d.status = 'UPLOADED'
    """)
    List<Document> findAccessibleDocuments(Long courseId, EducationLevel educationLevel, GradeLevel gradeLevel);

    @Query("""
     SELECT DISTINCT d
     FROM Document d
     JOIN d.targets t
     WHERE t.id.courseId = :courseId
       AND d.status = 'UPLOADED'
    """)
    List<Document> findAllByCourseId(Long courseId);
}
