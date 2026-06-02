package com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
     SELECT DISTINCT d
     FROM Document d
     JOIN d.targets t
     WHERE t.id.courseId = :courseId
       AND t.id.educationLevel = :educationLevel
       AND t.id.gradeLevel = :gradeLevel
       AND d.status IN ('UPLOADED', 'PROCESSING', 'READY')
    """)
    List<Document> findAccessibleDocuments(Long courseId, EducationLevel educationLevel, GradeLevel gradeLevel);

    @Query("""
     SELECT DISTINCT d
     FROM Document d
     JOIN d.targets t
     WHERE t.id.courseId = :courseId
       AND d.status IN ('UPLOADED', 'PROCESSING', 'READY')
    """)
    List<Document> findAllByCourseId(Long courseId);

    @Query("""
        SELECT DISTINCT d
        FROM Document d
        LEFT JOIN FETCH d.targets t
        WHERE d.id = :documentId
          AND d.status IN ('UPLOADED', 'PROCESSING', 'READY')
    """)
    Optional<Document> findByIdWithTargets(Long documentId);

    @Query("""
        SELECT d
        FROM Document d
        WHERE d.fileStorageInfo.fileChecksum = :checksum
    """)
    Optional<Document> findByChecksum(String checksum);

}
