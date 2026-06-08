package com.hs.hstesis.notifications.interfaces.rest;

import com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.hs.hstesis.notifications.domain.model.queries.GetUnreadNotificationsByUserIdQuery;
import com.hs.hstesis.notifications.domain.services.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Endpoints for user notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    public NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @Operation(description = "Get unread notifications for the authenticated user")
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Unauthorized"));
        }

        var userDetails = (UserDetailsImpl) authentication.getPrincipal();
        var query = new GetUnreadNotificationsByUserIdQuery(userDetails.getId());
        var notifications = notificationQueryService.handle(query);
        
        var notificationResources = notifications.stream()
                .map(com.hs.hstesis.notifications.interfaces.rest.transform.NotificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        
        return ResponseEntity.ok(notificationResources);
    }
}
