package com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.repo.domain.model.entities.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    @Query(value = """
            SELECT c.content
            FROM document_chunks c
            JOIN documents d ON c.document_id = d.id
            JOIN document_targets t ON d.id = t.document_id
            WHERE t.course_id = :courseId
            ORDER BY c.embedding <=> cast(:vector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<String> findSimilarChunksByCourseId(@Param("courseId") Long courseId,
                                             @Param("vector") String vector,
                                             @Param("limit") int limit);

    @Query(value = """
            SELECT c.content
            FROM document_chunks c
            JOIN documents d ON c.document_id = d.id
            JOIN document_targets t ON d.id = t.document_id
            WHERE t.course_id IN (:courseIds)
            ORDER BY c.embedding <=> cast(:vector as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<String> findSimilarChunksByCourseIdsIn(@Param("courseIds") List<Long> courseIds,
                                                @Param("vector") String vector,
                                                @Param("limit") int limit);

    List<DocumentChunk> findAllByDocumentTopicId(Long topicId);

    List<DocumentChunk> findAllByDocumentTopicIdIn(List<Long> topicIds);
}
