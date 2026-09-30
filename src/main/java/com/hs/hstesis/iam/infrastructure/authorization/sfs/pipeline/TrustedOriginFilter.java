package com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/** Protect cookie-authenticated writes without requiring JavaScript access to HttpOnly tokens. */
public class TrustedOriginFilter extends OncePerRequestFilter {
    private final Set<String> allowedOrigins;
    public TrustedOriginFilter(Set<String> allowedOrigins) { this.allowedOrigins = Set.copyOf(allowedOrigins); }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean write = !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
        boolean hasAuthCookie = request.getCookies() != null && java.util.Arrays.stream(request.getCookies())
                .anyMatch(c -> Set.of("JWT_TOKEN", "REFRESH_TOKEN").contains(c.getName()));
        boolean bearer = request.getHeader("Authorization") != null && request.getHeader("Authorization").startsWith("Bearer ");
        String origin = request.getHeader("Origin");
        if (write && ((origin != null && !allowedOrigins.contains(origin))
                || (hasAuthCookie && !bearer && origin == null))) {
            response.sendError(403, "Untrusted request origin");
            return;
        }
        chain.doFilter(request, response);
    }
}
