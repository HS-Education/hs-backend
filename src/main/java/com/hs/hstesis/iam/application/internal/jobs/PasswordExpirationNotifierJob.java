package com.hs.hstesis.iam.application.internal.jobs;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.UserRepository;
import com.hs.hstesis.notifications.domain.model.commands.CreateNotificationCommand;
import com.hs.hstesis.notifications.domain.services.NotificationCommandService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class PasswordExpirationNotifierJob {

    private final UserRepository userRepository;
    private final NotificationCommandService notificationCommandService;

    public PasswordExpirationNotifierJob(UserRepository userRepository, NotificationCommandService notificationCommandService) {
        this.userRepository = userRepository;
        this.notificationCommandService = notificationCommandService;
    }

    // Run every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkPasswordExpirations() {
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> u.getLastPasswordChange() != null)
                .toList();

        LocalDateTime now = LocalDateTime.now();

        for (User user : activeUsers) {
            long daysSinceChange = ChronoUnit.DAYS.between(user.getLastPasswordChange(), now);

            // Wait 60 days. Then count 30 days down. So total 90 days.
            if (daysSinceChange == 60) {
                sendNotification(user, "Su contraseña caducará en 30 días.");
            } else if (daysSinceChange == 75) {
                sendNotification(user, "Su contraseña caducará en 15 días.");
            } else if (daysSinceChange == 85) {
                sendNotification(user, "Su contraseña caducará en 5 días.");
            } else if (daysSinceChange == 89) {
                sendNotification(user, "Su contraseña caduca mañana.");
            } else if (daysSinceChange >= 90) {
                // We might lock them here or let the login block them. The login is already blocking them.
                sendNotification(user, "Su contraseña ha caducado. Debe cambiarla en su próximo inicio de sesión.");
            }
        }
    }

    private void sendNotification(User user, String message) {
        var command = new CreateNotificationCommand(user.getId(), message);
        notificationCommandService.handle(command);
    }
}
