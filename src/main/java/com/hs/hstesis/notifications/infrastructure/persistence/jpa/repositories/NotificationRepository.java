package com.hs.hstesis.notifications.infrastructure.persistence.jpa.repositories;

import com.hs.hstesis.notifications.domain.model.aggregates.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndIsReadFalseAndTypeIn(
            Long userId,
            List<com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType> types);
}
