package com.hs.hstesis.notifications.domain.model.aggregates;

import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Entity;
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
    private boolean isRead;

    public Notification() {}

    public Notification(Long userId, String message) {
        this.userId = userId;
        this.message = message;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
