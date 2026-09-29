package com.hs.hstesis.shared.domain.model.entities;

import com.hs.hstesis.shared.domain.model.valueobjects.AiBackgroundTaskStatus;
import com.hs.hstesis.shared.domain.model.valueobjects.AiBackgroundTaskType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiBackgroundTaskTest {
    @Test
    void taskRetriesAutomaticallyThreeTimesThenCanBeRetriedManually() {
        var task = new AiBackgroundTask(AiBackgroundTaskType.STUDENT_INSIGHT, 42L, "student-insight-1");

        task.markProcessing();
        assertThat(task.markAttemptFailed()).isFalse();
        assertThat(task.getStatus()).isEqualTo(AiBackgroundTaskStatus.PENDING);
        assertThat(task.getAvailableAt()).isAfter(java.time.LocalDateTime.now());

        task.markProcessing();
        assertThat(task.markAttemptFailed()).isFalse();
        task.markProcessing();
        assertThat(task.markAttemptFailed()).isTrue();
        assertThat(task.getStatus()).isEqualTo(AiBackgroundTaskStatus.FAILED);

        task.retryManually();
        assertThat(task.getStatus()).isEqualTo(AiBackgroundTaskStatus.PENDING);
        assertThat(task.getAttemptCount()).isZero();
    }
}
