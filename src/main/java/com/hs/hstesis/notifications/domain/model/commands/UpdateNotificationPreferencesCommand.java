package com.hs.hstesis.notifications.domain.model.commands;

public record UpdateNotificationPreferencesCommand(
        Long userId,
        boolean notifyNewQuestionnaire,
        boolean notifyNewTutorial,
        boolean notifyLowPerformance
) {}
