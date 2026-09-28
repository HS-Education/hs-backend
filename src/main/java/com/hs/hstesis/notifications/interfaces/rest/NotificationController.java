package com.hs.hstesis.notifications.interfaces.rest;

import com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.hs.hstesis.notifications.domain.model.queries.GetUnreadNotificationsByUserIdQuery;
import com.hs.hstesis.notifications.domain.services.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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
    private final com.hs.hstesis.notifications.domain.services.NotificationCommandService notificationCommandService;
    private final com.hs.hstesis.notifications.infrastructure.sse.NotificationSseRegistry sseRegistry;

    public NotificationController(
            NotificationQueryService notificationQueryService, 
            com.hs.hstesis.notifications.domain.services.NotificationCommandService notificationCommandService,
            com.hs.hstesis.notifications.infrastructure.sse.NotificationSseRegistry sseRegistry) {
        this.notificationQueryService = notificationQueryService;
        this.notificationCommandService = notificationCommandService;
        this.sseRegistry = sseRegistry;
    }

    @Operation(description = "Open the server-sent events stream for the authenticated user")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        }

        var userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return sseRegistry.subscribe(userDetails.getId());
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

    @Operation(description = "Mark a notification as read")
    @org.springframework.web.bind.annotation.PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@org.springframework.web.bind.annotation.PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Unauthorized"));
        }

        var userDetails = (UserDetailsImpl) authentication.getPrincipal();
        var command = new com.hs.hstesis.notifications.domain.model.commands.MarkNotificationAsReadCommand(id, userDetails.getId());
        
        try {
            notificationCommandService.handle(command);
            return ResponseEntity.ok(java.util.Map.of("message", "Notification marked as read"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
