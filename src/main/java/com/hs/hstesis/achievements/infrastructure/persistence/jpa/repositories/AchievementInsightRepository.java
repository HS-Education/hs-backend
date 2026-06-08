package com.hs.hstesis.achievements.infrastructure.persistence.jpa.repositories;

import com.hs.hstesis.achievements.domain.model.aggregates.AchievementInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AchievementInsightRepository extends JpaRepository<AchievementInsight, Long> {
    Optional<AchievementInsight> findTopByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
}
