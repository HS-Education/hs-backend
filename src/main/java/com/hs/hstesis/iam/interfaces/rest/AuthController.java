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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Endpoints for user authentication and token management")
public class AuthController {

    private final SignInCommandService signInCommandService;
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;

    public AuthController(SignInCommandService signInCommandService, RefreshTokenService refreshTokenService, TokenService tokenService) {
        this.signInCommandService = signInCommandService;
        this.refreshTokenService = refreshTokenService;
        this.tokenService = tokenService;
    }

    @PostMapping("/sign-in")
    public ResponseEntity<AuthenticatedUserResource> signIn(@RequestBody SignInResource resource) {

        var signInCommand = SignInCommandFromResourceAssembler.toCommandFromResource(resource);
        var authenticatedUser = signInCommandService.handle(signInCommand);

        if (authenticatedUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var user = authenticatedUser.get().getLeft();
        var token = authenticatedUser.get().getRight();

        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        ResponseCookie jwtCookie = ResponseCookie.from("JWT_TOKEN", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(900)
                .sameSite("Strict")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken.getToken())
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth/refresh-token")
                .maxAge(604800000)
                .sameSite("Strict")
                .build();

        var responseResource = AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(user);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(responseResource);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String requestRefreshToken = refreshTokenService.getRefreshTokenFromCookie(request);

        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No refresh token provided"));
        }

        try {
            return refreshTokenService.findByToken(requestRefreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUser)
                    .map(user -> {
                        String newToken = tokenService.generateTokenFromUser(user);

                        ResponseCookie jwtCookie = ResponseCookie.from("JWT_TOKEN", newToken)
                                .httpOnly(true)
                                .secure(false)
                                .path("/")
                                .maxAge(900)
                                .sameSite("Strict")
                                .build();

                        return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                                .body(Map.of("message", "Token refreshed successfully"));
                    })
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }


    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var userDetails = (UserDetailsImpl) authentication.getPrincipal();

        assert userDetails != null;
        return ResponseEntity.ok(Map.of(
                "id", userDetails.getId(),
                "username", userDetails.getUsername(),
                "roles", userDetails.getRoles()
        ));
    }

    @PostMapping("/log-out")
    public ResponseEntity<Void> logOut() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            refreshTokenService.deleteByUserId(userDetails.getId());
        }

        ResponseCookie jwtCookie = ResponseCookie.from("JWT_TOKEN", "").path("/").maxAge(0).build();
        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", "").path("/api/v1/auth/refresh-token").maxAge(0).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .build();
    }
}
