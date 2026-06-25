package com.hs.hstesis.onboarding.infrastructure.persistance.jpa.repositories;

import com.hs.hstesis.onboarding.domain.model.aggregates.OnboardingProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingRepository extends JpaRepository<OnboardingProfile, Long> {
    Optional<OnboardingProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
