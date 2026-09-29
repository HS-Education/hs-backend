package com.hs.hstesis.notifications.interfaces.rest.transform;

import com.hs.hstesis.notifications.domain.model.commands.UpdateNotificationPreferencesCommand;
import com.hs.hstesis.notifications.interfaces.rest.resources.UpdateNotificationPreferenceResource;

public class UpdateNotificationPreferencesCommandFromResourceAssembler {
    public static UpdateNotificationPreferencesCommand toCommandFromResource(Long userId, UpdateNotificationPreferenceResource resource) {
        return new UpdateNotificationPreferencesCommand(
                userId,
                resource.notifyNewQuestionnaire(),
                resource.notifyNewTutorial(),
                resource.notifyLowPerformance()
        );
    }
}
