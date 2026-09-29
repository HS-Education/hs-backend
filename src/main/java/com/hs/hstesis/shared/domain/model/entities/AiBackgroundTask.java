package com.hs.hstesis.shared.domain.model.entities;

import com.hs.hstesis.shared.domain.model.valueobjects.AiBackgroundTaskStatus;
import com.hs.hstesis.shared.domain.model.valueobjects.AiBackgroundTaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_background_tasks", indexes = {
        @Index(name = "idx_ai_tasks_pending", columnList = "task_type,status,available_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_ai_task_idempotency_key", columnNames = "idempotency_key"))
@Getter
@NoArgsConstructor
public class AiBackgroundTask {
    private static final int MAX_AUTOMATIC_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 40)
    private AiBackgroundTaskType taskType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiBackgroundTaskStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "last_error", length = 120)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AiBackgroundTask(AiBackgroundTaskType taskType, Long subjectId, String idempotencyKey) {
        this.taskType = taskType;
        this.subjectId = subjectId;
        this.idempotencyKey = idempotencyKey;
        this.status = AiBackgroundTaskStatus.PENDING;
        this.availableAt = LocalDateTime.now();
    }

    public void markProcessing() {
        this.status = AiBackgroundTaskStatus.PROCESSING;
        this.attemptCount++;
        this.lastError = null;
    }

    public void markCompleted() {
        this.status = AiBackgroundTaskStatus.COMPLETED;
        this.lastError = null;
    }

    public boolean markAttemptFailed() {
        this.lastError = "AI_GENERATION_FAILED";
        if (attemptCount >= MAX_AUTOMATIC_ATTEMPTS) {
            this.status = AiBackgroundTaskStatus.FAILED;
            return true;
        }

        this.status = AiBackgroundTaskStatus.PENDING;
        this.availableAt = LocalDateTime.now().plusSeconds(30L * (1L << (attemptCount - 1)));
        return false;
    }

    public void retryManually() {
        if (this.status != AiBackgroundTaskStatus.FAILED) {
            throw new IllegalStateException("Only failed AI tasks can be retried.");
        }
        this.status = AiBackgroundTaskStatus.PENDING;
        this.attemptCount = 0;
        this.availableAt = LocalDateTime.now();
        this.lastError = null;
    }

    @PrePersist
    void initializeTimestamps() {
        var now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
