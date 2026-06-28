package com.hs.hstesis.notifications.domain.model.aggregates;

import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class NotificationPreference extends AuditableAbstractAggregateRoot<NotificationPreference> {

    private Long userId;

    private boolean notifyQuizResults;
    private boolean notifyRelevantActivity;
    private boolean notifyNewDocument;
    private boolean notifyUnresolvedQuizzes;

    public NotificationPreference() {
    }

    public NotificationPreference(Long userId) {
        this.userId = userId;
        this.notifyQuizResults = true;
        this.notifyRelevantActivity = true;
        this.notifyNewDocument = true;
        this.notifyUnresolvedQuizzes = true;
    }

    public void updatePreferences(boolean notifyQuizResults, boolean notifyRelevantActivity, boolean notifyNewDocument, boolean notifyUnresolvedQuizzes) {
        this.notifyQuizResults = notifyQuizResults;
        this.notifyRelevantActivity = notifyRelevantActivity;
        this.notifyNewDocument = notifyNewDocument;
        this.notifyUnresolvedQuizzes = notifyUnresolvedQuizzes;
    }
}
