package com.hs.hstesis.achievements.application.internal.commandservices;

import com.hs.hstesis.achievements.domain.model.commands.GenerateStudentInsightCommand;
import com.hs.hstesis.achievements.domain.services.AchievementCommandService;
import com.hs.hstesis.shared.domain.model.valueobjects.AiBackgroundTaskType;
import com.hs.hstesis.shared.infrastructure.persistence.jpa.repositories.AiBackgroundTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class StudentInsightTaskWorker {
    private static final Logger log = LoggerFactory.getLogger(StudentInsightTaskWorker.class);

    private final AiBackgroundTaskRepository tasks;
    private final AchievementCommandService achievementCommandService;

    public StudentInsightTaskWorker(AiBackgroundTaskRepository tasks,
                                    AchievementCommandService achievementCommandService) {
        this.tasks = tasks;
        this.achievementCommandService = achievementCommandService;
    }

    @Scheduled(fixedDelayString = "${ai.background-tasks.poll-interval-ms:5000}")
    @Transactional
    public void processNextStudentInsightTask() {
        var task = tasks.lockNextReadyTask(AiBackgroundTaskType.STUDENT_INSIGHT.name(), LocalDateTime.now())
                .orElse(null);
        if (task == null) return;

        task.markProcessing();
        try {
            achievementCommandService.handle(new GenerateStudentInsightCommand(task.getSubjectId()));
            task.markCompleted();
        } catch (RuntimeException exception) {
            boolean exhausted = task.markAttemptFailed();
            log.warn("Student insight generation failed; retryable={}", !exhausted);
        }
    }
}
