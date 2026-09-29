package com.hs.hstesis.shared.infrastructure.persistence.jpa.repositories;

import com.hs.hstesis.shared.domain.model.entities.AiBackgroundTask;
import com.hs.hstesis.shared.domain.model.valueobjects.AiBackgroundTaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AiBackgroundTaskRepository extends JpaRepository<AiBackgroundTask, Long> {
    Optional<AiBackgroundTask> findByIdempotencyKey(String idempotencyKey);

    @Query(value = """
            SELECT * FROM ai_background_tasks
            WHERE task_type = :taskType AND status = 'PENDING' AND available_at <= :now
            ORDER BY created_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<AiBackgroundTask> lockNextReadyTask(@Param("taskType") String taskType,
                                                  @Param("now") LocalDateTime now);
}
