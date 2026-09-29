package com.hs.hstesis.notifications.interfaces.rest.resources;

public record NotificationPreferenceResource(
        Long userId,
        boolean notifyNewQuestionnaire,
        boolean notifyNewTutorial,
        boolean notifyLowPerformance
) {}
