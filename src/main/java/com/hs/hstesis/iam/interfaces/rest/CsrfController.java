package com.hs.hstesis.iam.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
    public record TokenResource(String token, String headerName) { }

    @GetMapping("/api/v1/auth/csrf")
    public ResponseEntity<TokenResource> csrf(HttpServletRequest request) {
        var token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        // Access materializes the deferred cookie. XOR masking is provided by Spring Security.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new TokenResource(token.getToken(), token.getHeaderName()));
    }
}
