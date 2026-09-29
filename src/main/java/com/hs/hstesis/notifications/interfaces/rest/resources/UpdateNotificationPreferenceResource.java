package com.hs.hstesis.notifications.interfaces.rest.resources;

public record UpdateNotificationPreferenceResource(
        boolean notifyNewQuestionnaire,
        boolean notifyNewTutorial,
        boolean notifyLowPerformance
) {}
