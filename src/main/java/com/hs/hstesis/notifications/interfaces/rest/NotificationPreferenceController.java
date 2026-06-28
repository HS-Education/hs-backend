package com.hs.hstesis.notifications.interfaces.rest;

import com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.hs.hstesis.notifications.domain.model.queries.GetNotificationPreferencesQuery;
import com.hs.hstesis.notifications.domain.services.NotificationPreferenceCommandService;
import com.hs.hstesis.notifications.domain.services.NotificationPreferenceQueryService;
import com.hs.hstesis.notifications.interfaces.rest.resources.NotificationPreferenceResource;
import com.hs.hstesis.notifications.interfaces.rest.resources.UpdateNotificationPreferenceResource;
import com.hs.hstesis.notifications.interfaces.rest.transform.NotificationPreferenceResourceFromEntityAssembler;
import com.hs.hstesis.notifications.interfaces.rest.transform.UpdateNotificationPreferencesCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@Tag(name = "Notification Preferences", description = "Endpoints for user notification preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceCommandService commandService;
    private final NotificationPreferenceQueryService queryService;

    public NotificationPreferenceController(NotificationPreferenceCommandService commandService, NotificationPreferenceQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notification preferences for the authenticated user")
    public ResponseEntity<NotificationPreferenceResource> getPreferences(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        var query = new GetNotificationPreferencesQuery(userDetails.getId());
        var preference = queryService.handle(query);
        if (preference.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(NotificationPreferenceResourceFromEntityAssembler.toResourceFromEntity(preference.get()));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update notification preferences for the authenticated user")
    public ResponseEntity<NotificationPreferenceResource> updatePreferences(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UpdateNotificationPreferenceResource resource) {
        var command = UpdateNotificationPreferencesCommandFromResourceAssembler.toCommandFromResource(userDetails.getId(), resource);
        var updatedPreference = commandService.handle(command);
        return ResponseEntity.ok(NotificationPreferenceResourceFromEntityAssembler.toResourceFromEntity(updatedPreference));
    }
}
