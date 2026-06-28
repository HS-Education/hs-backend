package com.hs.hstesis.notifications.domain.model.commands;

public record UpdateNotificationPreferencesCommand(
        Long userId,
        boolean notifyQuizResults,
        boolean notifyRelevantActivity,
        boolean notifyNewDocument,
        boolean notifyUnresolvedQuizzes
) {}
