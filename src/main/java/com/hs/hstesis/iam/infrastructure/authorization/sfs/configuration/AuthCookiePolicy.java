package com.hs.hstesis.iam.infrastructure.authorization.sfs.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookiePolicy {
    private final boolean secure;
    private final String sameSite;
    private final long accessSeconds;
    private final long refreshSeconds;

    public AuthCookiePolicy(@Value("${app.auth.cookie-secure:false}") boolean secure,
                            @Value("${app.auth.cookie-same-site:Strict}") String sameSite,
                            @Value("${jwt.expiration-minutes:60}") long accessMinutes,
                            @Value("${jwt.refresh-expiration-days:7}") long refreshDays) {
        if (!java.util.Set.of("Strict", "Lax", "None").contains(sameSite)
                || ("None".equals(sameSite) && !secure) || accessMinutes < 1 || refreshDays < 1) {
            throw new IllegalArgumentException("Invalid authentication cookie policy");
        }
        this.secure = secure;
        this.sameSite = sameSite;
        this.accessSeconds = accessMinutes * 60;
        this.refreshSeconds = refreshDays * 86400;
    }

    public ResponseCookie access(String token) { return cookie("JWT_TOKEN", token, "/", accessSeconds); }
    public ResponseCookie refresh(String token) { return cookie("REFRESH_TOKEN", token, "/api/v1/auth/refresh-token", refreshSeconds); }
    public ResponseCookie clearAccess() { return cookie("JWT_TOKEN", "", "/", 0); }
    public ResponseCookie clearRefresh() { return cookie("REFRESH_TOKEN", "", "/api/v1/auth/refresh-token", 0); }
    // Match CookieCsrfTokenRepository: rotate only at explicit authentication transitions.
    public ResponseCookie clearCsrf() {
        return ResponseCookie.from("XSRF-TOKEN", "").httpOnly(true).secure(secure).sameSite("Strict")
                .path("/").maxAge(0).build();
    }

    private ResponseCookie cookie(String name, String token, String path, long seconds) {
        return ResponseCookie.from(name, token).httpOnly(true).secure(secure).sameSite(sameSite)
                .path(path).maxAge(seconds).build();
    }
}
