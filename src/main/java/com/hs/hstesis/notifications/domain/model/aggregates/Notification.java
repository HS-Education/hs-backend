package com.hs.hstesis.notifications.domain.model.aggregates;

import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "notifications")
public class Notification extends AuditableAbstractAggregateRoot<Notification> {
    
    private Long userId;
    private String message;
    @Enumerated(EnumType.STRING)
    private com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType type;
    private boolean isRead;

    public Notification() {}

    public Notification(Long userId, String message, com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType type) {
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
