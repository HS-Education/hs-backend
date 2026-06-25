package com.hs.hstesis.onboarding.domain.model.aggregates;

import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_profiles")
@Getter
@Setter
public class OnboardingProfile extends AuditableAbstractAggregateRoot<OnboardingProfile> {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected OnboardingProfile() {}

    public OnboardingProfile(Long userId) {
        this.userId = userId;
        this.completed = false;
        this.completedAt = null;
    }

    public void markCompleted() {
        this.completed = true;
        this.completedAt = LocalDateTime.now();
    }
}
