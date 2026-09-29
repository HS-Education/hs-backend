package com.hs.hstesis.achievements.application.internal.commandservices;

import com.hs.hstesis.achievements.domain.model.commands.GenerateClassroomInsightCommand;
import com.hs.hstesis.achievements.domain.model.queries.GetClassroomPerformanceQuery;
import com.hs.hstesis.achievements.domain.services.AchievementCommandService;
import com.hs.hstesis.achievements.domain.services.AchievementQueryService;
import com.hs.hstesis.achievements.infrastructure.persistence.jpa.repositories.AchievementInsightRepository;
import com.hs.hstesis.learning.domain.model.queries.GetAllClassroomsQuery;
import com.hs.hstesis.learning.domain.model.valueobjects.ClassroomStatus;
import com.hs.hstesis.learning.domain.services.ClassroomQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Component
public class ClassroomInsightScheduler {
    private static final Logger log = LoggerFactory.getLogger(ClassroomInsightScheduler.class);

    private final ClassroomQueryService classroomQueryService;
    private final AchievementQueryService achievementQueryService;
    private final AchievementCommandService achievementCommandService;
    private final AchievementInsightRepository insightRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public ClassroomInsightScheduler(ClassroomQueryService classroomQueryService,
                                     AchievementQueryService achievementQueryService,
                                     AchievementCommandService achievementCommandService,
                                     AchievementInsightRepository insightRepository,
                                     JdbcTemplate jdbcTemplate,
                                     PlatformTransactionManager transactionManager) {
        this.classroomQueryService = classroomQueryService;
        this.achievementQueryService = achievementQueryService;
        this.achievementCommandService = achievementCommandService;
        this.insightRepository = insightRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // Check hourly so each active classroom receives a new report once its previous report is seven days old.
    @Scheduled(fixedDelayString = "3600000", initialDelayString = "60000")
    public void generateDueClassroomInsights() {
        var cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        for (var classroom : classroomQueryService.handle(new GetAllClassroomsQuery())) {
            if (classroom.getStatus() != ClassroomStatus.ACTIVE) continue;
            var classroomId = classroom.getId();
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    // PostgreSQL transaction lock prevents duplicate reports when several backend instances run the job.
                    var lockKey = classroomId ^ 0x5345525900000000L;
                    var locked = jdbcTemplate.queryForObject("SELECT pg_try_advisory_xact_lock(?)", Boolean.class, lockKey);
                    if (!Boolean.TRUE.equals(locked)) return;

                    var latest = insightRepository.findTopByEntityTypeAndEntityIdOrderByCreatedAtDesc("CLASSROOM", classroomId);
                    if (latest.isPresent() && latest.get().getCreatedAt() != null
                            && latest.get().getCreatedAt().toInstant().isAfter(cutoff)) return;

                    var performance = achievementQueryService.handle(new GetClassroomPerformanceQuery(classroomId));
                    if (performance.isEmpty() || performance.get().students().stream().noneMatch(student ->
                            student.topics().stream().anyMatch(topic -> Objects.equals(topic.courseId(), classroom.getCourse().getId())))) return;

                    achievementCommandService.handle(new GenerateClassroomInsightCommand(classroomId));
                });
            } catch (Exception exception) {
                log.warn("Could not generate scheduled insight for classroom {}", classroomId, exception);
            }
        }
    }
}
