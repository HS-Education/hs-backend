package com.hs.hstesis.notifications.interfaces.rest.resources;

public record UpdateNotificationPreferenceResource(
        boolean notifyQuizResults,
        boolean notifyRelevantActivity,
        boolean notifyNewDocument,
        boolean notifyUnresolvedQuizzes
) {}
