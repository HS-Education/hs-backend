package com.hs.hstesis.onboarding.interfaces.rest;

import com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.hs.hstesis.onboarding.domain.model.commands.CompleteOnboardingCommand;
import com.hs.hstesis.onboarding.domain.model.queries.GetOnboardingStatusQuery;
import com.hs.hstesis.onboarding.domain.services.OnboardingCommandService;
import com.hs.hstesis.onboarding.domain.services.OnboardingQueryService;
import com.hs.hstesis.onboarding.interfaces.rest.resources.OnboardingStatusResource;
import com.hs.hstesis.shared.interfaces.rest.resources.MessageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/onboarding", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Onboarding", description = "Endpoints to manage user onboarding status")
public class OnboardingController {

    private final OnboardingCommandService onboardingCommandService;
    private final OnboardingQueryService onboardingQueryService;

    public OnboardingController(OnboardingCommandService onboardingCommandService,
                                OnboardingQueryService onboardingQueryService) {
        this.onboardingCommandService = onboardingCommandService;
        this.onboardingQueryService = onboardingQueryService;
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(description = "Returns the onboarding status of the currently authenticated user.")
    @GetMapping("/status")
    public ResponseEntity<OnboardingStatusResource> getStatus() {
        Long userId = getAuthenticatedUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        var result = onboardingQueryService.handle(new GetOnboardingStatusQuery(userId));
        // If no record exists yet, the user has not completed onboarding
        boolean completed = result.map(p -> p.isCompleted()).orElse(false);
        boolean quizzesCompleted = result.map(p -> p.isQuizzesCompleted()).orElse(false);
        boolean repositoryCompleted = result.map(p -> p.isRepositoryCompleted()).orElse(false);
        return ResponseEntity.ok(new OnboardingStatusResource(completed, quizzesCompleted, repositoryCompleted));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(description = "Marks the onboarding as completed for the currently authenticated user.")
    @PostMapping("/complete")
    public ResponseEntity<MessageResource> complete() {
        Long userId = getAuthenticatedUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        onboardingCommandService.handle(new CompleteOnboardingCommand(userId));
        return ResponseEntity.ok(new MessageResource("Onboarding completed successfully"));
    }
    @PreAuthorize("isAuthenticated()")
    @Operation(description = "Marks the quizzes onboarding as completed for the currently authenticated user.")
    @PostMapping("/complete/quizzes")
    public ResponseEntity<MessageResource> completeQuizzes() {
        Long userId = getAuthenticatedUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        onboardingCommandService.handle(new com.hs.hstesis.onboarding.domain.model.commands.MarkQuizzesOnboardingCompletedCommand(userId));
        return ResponseEntity.ok(new MessageResource("Quizzes onboarding completed successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(description = "Marks the repository onboarding as completed for the currently authenticated user.")
    @PostMapping("/complete/repository")
    public ResponseEntity<MessageResource> completeRepository() {
        Long userId = getAuthenticatedUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        onboardingCommandService.handle(new com.hs.hstesis.onboarding.domain.model.commands.MarkRepositoryOnboardingCompletedCommand(userId));
        return ResponseEntity.ok(new MessageResource("Repository onboarding completed successfully"));
    }
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        var userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId();
    }
}
