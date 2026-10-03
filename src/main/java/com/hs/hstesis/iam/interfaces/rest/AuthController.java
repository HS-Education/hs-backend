package com.hs.hstesis.iam.interfaces.rest;

import com.hs.hstesis.iam.application.internal.outboundservices.tokens.TokenService;
import com.hs.hstesis.iam.domain.model.entity.RefreshToken;
import com.hs.hstesis.iam.domain.services.SignInCommandService;
import com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.hs.hstesis.iam.infrastructure.tokens.jwt.RefreshTokenService;
import com.hs.hstesis.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.hs.hstesis.iam.interfaces.rest.resources.SignInResource;
import com.hs.hstesis.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import com.hs.hstesis.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import com.hs.hstesis.shared.interfaces.rest.resources.MessageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Endpoints for user authentication and token management")
public class AuthController {

    private final SignInCommandService signInCommandService;
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;
    private final com.hs.hstesis.iam.infrastructure.authorization.sfs.configuration.AuthCookiePolicy cookiePolicy;
    private final com.hs.hstesis.iam.domain.services.UserCommandService userCommandService;

    public AuthController(SignInCommandService signInCommandService, RefreshTokenService refreshTokenService, TokenService tokenService, com.hs.hstesis.iam.domain.services.UserCommandService userCommandService, com.hs.hstesis.iam.infrastructure.authorization.sfs.configuration.AuthCookiePolicy cookiePolicy) {
        this.signInCommandService = signInCommandService;
        this.refreshTokenService = refreshTokenService;
        this.tokenService = tokenService;
        this.userCommandService = userCommandService;
        this.cookiePolicy = cookiePolicy;
    }

    @Operation(description = "Authenticates a user and returns a JWT token along with a refresh token in HttpOnly cookies.")
    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody SignInResource resource) {

        var signInCommand = SignInCommandFromResourceAssembler.toCommandFromResource(resource);
        var authenticatedUser = signInCommandService.handle(signInCommand);

        if (authenticatedUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var user = authenticatedUser.get().getLeft();
        
        if (user.isTemporaryPassword()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                java.util.Map.of("message", "Debe cambiar su contraseña", "reason", "TEMPORARY_PASSWORD")
            );
        }
        
        if (user.getLastPasswordChange() != null && user.getLastPasswordChange().plusDays(90).isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                java.util.Map.of("message", "Debe cambiar su contraseña", "reason", "PASSWORD_EXPIRED")
            );
        }

        var token = authenticatedUser.get().getRight();

        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        ResponseCookie jwtCookie = cookiePolicy.access(token);
        ResponseCookie refreshCookie = cookiePolicy.refresh(refreshToken.getToken());

        var responseResource = AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(user);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, cookiePolicy.clearCsrf().toString())
                .body(responseResource);
    }

    @Operation(description = "Refreshes the JWT token using a valid refresh token.")
    @PostMapping("/refresh-token")
    public ResponseEntity<MessageResource> refreshToken(HttpServletRequest request) {
        String requestRefreshToken = refreshTokenService.getRefreshTokenFromCookie(request);

        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResource("No refresh token provided"));
        }

        try {
            return refreshTokenService.findByToken(requestRefreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUser)
                    .map(user -> {
                        String newToken = tokenService.generateTokenFromUser(user);

                        ResponseCookie jwtCookie = cookiePolicy.access(newToken);

                        return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                                .header(HttpHeaders.SET_COOKIE, cookiePolicy.clearCsrf().toString())
                                .body(new MessageResource("Token refreshed successfully"));
                    })
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResource(e.getMessage()));
        }
    }

    @Operation(description = "Returns the details of the currently authenticated user based on the JWT token.")
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResource> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var userDetails = (UserDetailsImpl) authentication.getPrincipal();

        assert userDetails != null;
        var responseResource = new AuthenticatedUserResource(
                userDetails.getId(),
                userDetails.getName(),
                userDetails.getUsername(),
                userDetails.getRoles()
        );
        return ResponseEntity.ok(responseResource);
    }

    @Operation(description = "Logs out the user by deleting the refresh token and clearing the JWT and refresh token cookies.")
    @PostMapping("/log-out")
    public ResponseEntity<MessageResource> logOut() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            refreshTokenService.deleteByUserId(userDetails.getId());
        }

        ResponseCookie jwtCookie = cookiePolicy.clearAccess();
        ResponseCookie refreshCookie = cookiePolicy.clearRefresh();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, cookiePolicy.clearCsrf().toString())
                .body(new MessageResource("Logged out successfully"));
    }

    @Operation(description = "Changes the user password.")
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody com.hs.hstesis.iam.interfaces.rest.resources.ChangePasswordResource resource) {
        try {
            var command = new com.hs.hstesis.iam.domain.model.commands.ChangePasswordCommand(resource.username(), resource.oldPassword(), resource.newPassword());
            userCommandService.handle(command);
            return ResponseEntity.ok(java.util.Map.of("message", "Contraseña cambiada exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
