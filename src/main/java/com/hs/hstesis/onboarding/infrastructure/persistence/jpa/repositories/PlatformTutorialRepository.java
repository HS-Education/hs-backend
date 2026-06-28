package com.hs.hstesis.onboarding.infrastructure.persistence.jpa.repositories;

import com.hs.hstesis.onboarding.domain.model.aggregates.PlatformTutorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformTutorialRepository extends JpaRepository<PlatformTutorial, Long> {
}

