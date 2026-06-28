package com.hs.hstesis.notifications.infrastructure.persistence.jpa.repositories;

import com.hs.hstesis.notifications.domain.model.aggregates.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
