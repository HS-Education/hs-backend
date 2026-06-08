package com.hs.hstesis.achievements.domain.model.aggregates;

import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "achievement_insights")
@Getter
@Setter
@NoArgsConstructor
public class AchievementInsight extends AuditableAbstractAggregateRoot<AchievementInsight> {

    // e.g., "STUDENT", "CLASSROOM", "AREA"
    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String insightText;

    public AchievementInsight(String entityType, Long entityId, String insightText) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.insightText = insightText;
    }
}
