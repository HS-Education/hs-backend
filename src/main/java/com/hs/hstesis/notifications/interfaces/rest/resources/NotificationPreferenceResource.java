package com.hs.hstesis.notifications.interfaces.rest.resources;

public record NotificationPreferenceResource(
        Long userId,
        boolean notifyQuizResults,
        boolean notifyRelevantActivity,
        boolean notifyNewDocument,
        boolean notifyUnresolvedQuizzes
) {}
