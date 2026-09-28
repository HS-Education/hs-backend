package com.hs.hstesis.notifications.application.internal.commandservices;

import com.hs.hstesis.notifications.domain.model.aggregates.NotificationPreference;
import com.hs.hstesis.notifications.domain.model.commands.UpdateNotificationPreferencesCommand;
import com.hs.hstesis.notifications.domain.services.NotificationPreferenceCommandService;
import com.hs.hstesis.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceCommandServiceImpl implements NotificationPreferenceCommandService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public NotificationPreferenceCommandServiceImpl(NotificationPreferenceRepository notificationPreferenceRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    @Override
    @Transactional
    public NotificationPreference handle(UpdateNotificationPreferencesCommand command) {
        var preference = notificationPreferenceRepository.findByUserId(command.userId())
                .orElseGet(() -> new NotificationPreference(command.userId()));

        preference.updatePreferences(
                command.notifyNewQuestionnaire(),
                command.notifyNewTutorial(),
                command.notifyLowPerformance()
        );

        return notificationPreferenceRepository.save(preference);
    }
}
