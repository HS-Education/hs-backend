package com.hs.hstesis.notifications.domain.model.aggregates;

import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
public class NotificationPreference extends AuditableAbstractAggregateRoot<NotificationPreference> {

    @jakarta.persistence.Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Legacy columns kept temporarily because existing databases still define them as NOT NULL.
     * They are not exposed by the current API and can be removed in a future schema migration.
     */
    @jakarta.persistence.Column(name = "notify_quiz_results", nullable = false)
    private boolean legacyNotifyQuizResults = true;

    @jakarta.persistence.Column(name = "notify_relevant_activity", nullable = false)
    private boolean legacyNotifyRelevantActivity = true;

    @jakarta.persistence.Column(name = "notify_new_document", nullable = false)
    private boolean legacyNotifyNewDocument = true;

    @jakarta.persistence.Column(name = "notify_unresolved_quizzes", nullable = false)
    private boolean legacyNotifyUnresolvedQuizzes = true;

    @jakarta.persistence.Column(columnDefinition = "boolean default true")
    private boolean notifyNewQuestionnaire;
    @jakarta.persistence.Column(columnDefinition = "boolean default true")
    private boolean notifyNewTutorial;
    @jakarta.persistence.Column(columnDefinition = "boolean default true")
    private boolean notifyLowPerformance;

    public NotificationPreference() {
    }

    public NotificationPreference(Long userId) {
        this.userId = userId;
        this.legacyNotifyQuizResults = true;
        this.legacyNotifyRelevantActivity = true;
        this.legacyNotifyNewDocument = true;
        this.legacyNotifyUnresolvedQuizzes = true;
        this.notifyNewQuestionnaire = true;
        this.notifyNewTutorial = true;
        this.notifyLowPerformance = true;
    }

    public void updatePreferences(boolean notifyNewQuestionnaire, boolean notifyNewTutorial, boolean notifyLowPerformance) {
        this.notifyNewQuestionnaire = notifyNewQuestionnaire;
        this.notifyNewTutorial = notifyNewTutorial;
        this.notifyLowPerformance = notifyLowPerformance;
    }

    public boolean isEnabled(com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType type) {
        return switch (type) {
            case NEW_QUESTIONNAIRE -> notifyNewQuestionnaire;
            case NEW_TUTORIAL -> notifyNewTutorial;
            case LOW_PERFORMANCE -> notifyLowPerformance;
        };
    }
}
