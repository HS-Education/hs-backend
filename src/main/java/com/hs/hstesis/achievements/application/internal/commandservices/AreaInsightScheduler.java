package com.hs.hstesis.achievements.application.internal.commandservices;

import com.hs.hstesis.achievements.domain.model.commands.GenerateAreaInsightCommand;
import com.hs.hstesis.achievements.domain.model.queries.GetAreaPerformanceQuery;
import com.hs.hstesis.achievements.domain.services.AchievementCommandService;
import com.hs.hstesis.achievements.domain.services.AchievementQueryService;
import com.hs.hstesis.achievements.infrastructure.persistence.jpa.repositories.AchievementInsightRepository;
import com.hs.hstesis.learning.domain.model.queries.GetAllAreasQuery;
import com.hs.hstesis.learning.domain.services.AreaQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class AreaInsightScheduler {
    private static final Logger log = LoggerFactory.getLogger(AreaInsightScheduler.class);
    private static final long AREA_LOCK_NAMESPACE = 0x4152454100000000L;

    private final AreaQueryService areaQueryService;
    private final AchievementQueryService achievementQueryService;
    private final AchievementCommandService achievementCommandService;
    private final AchievementInsightRepository insightRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public AreaInsightScheduler(AreaQueryService areaQueryService,
                                AchievementQueryService achievementQueryService,
                                AchievementCommandService achievementCommandService,
                                AchievementInsightRepository insightRepository,
                                JdbcTemplate jdbcTemplate,
                                PlatformTransactionManager transactionManager) {
        this.areaQueryService = areaQueryService;
        this.achievementQueryService = achievementQueryService;
        this.achievementCommandService = achievementCommandService;
        this.insightRepository = insightRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // Evaluate due areas hourly; the persisted insight timestamp enforces the seven-day cadence.
    @Scheduled(fixedDelayString = "3600000", initialDelayString = "60000")
    public void generateDueAreaInsights() {
        var cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        for (var areaModel : areaQueryService.handle(new GetAllAreasQuery())) {
            var areaId = areaModel.area().getId();
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    var lockKey = areaId ^ AREA_LOCK_NAMESPACE;
                    var locked = jdbcTemplate.queryForObject("SELECT pg_try_advisory_xact_lock(?)", Boolean.class, lockKey);
                    if (!Boolean.TRUE.equals(locked)) return;

                    var latest = insightRepository.findTopByEntityTypeAndEntityIdOrderByCreatedAtDesc("AREA", areaId);
                    if (latest.isPresent() && latest.get().getCreatedAt() != null
                            && latest.get().getCreatedAt().toInstant().isAfter(cutoff)) return;

                    var performance = achievementQueryService.handle(new GetAreaPerformanceQuery(areaId));
                    if (performance.isEmpty() || performance.get().classrooms().stream()
                            .flatMap(classroom -> classroom.students().stream())
                            .noneMatch(student -> !student.topics().isEmpty())) return;

                    achievementCommandService.handle(new GenerateAreaInsightCommand(areaId));
                });
            } catch (Exception exception) {
                log.warn("Could not generate scheduled insight for area {}", areaId, exception);
            }
        }
    }
}
