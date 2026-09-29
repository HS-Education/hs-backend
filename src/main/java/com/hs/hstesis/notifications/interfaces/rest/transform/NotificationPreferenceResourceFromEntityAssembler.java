package com.hs.hstesis.notifications.interfaces.rest.transform;

import com.hs.hstesis.notifications.domain.model.aggregates.NotificationPreference;
import com.hs.hstesis.notifications.interfaces.rest.resources.NotificationPreferenceResource;

public class NotificationPreferenceResourceFromEntityAssembler {
    public static NotificationPreferenceResource toResourceFromEntity(NotificationPreference entity) {
        return new NotificationPreferenceResource(
                entity.getUserId(),
                entity.isNotifyNewQuestionnaire(),
                entity.isNotifyNewTutorial(),
                entity.isNotifyLowPerformance()
        );
    }
}
