package com.hs.hstesis.iam.infrastructure.tokens.jwt.services;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.hs.hstesis.iam.infrastructure.tokens.jwt.BearerTokenService;
import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class TokenServiceImpl implements BearerTokenService {

    private final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);
    private final SecretKey signingKey;

    public TokenServiceImpl(SecretKey signingKey) {
        this.signingKey = signingKey;
    }

    @Value("${jwt.expiration-minutes:60}")
    private int expirationMinutes = 60;

    private String buildTokenWithDefaultParameters(String username, String userId, List<String> roles) {
        Date issuedAt = new Date();
        Date expiration = DateUtils.addMinutes(issuedAt, expirationMinutes);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String generateToken(Authentication authentication) {
        var userDetails = (UserDetailsImpl) authentication.getPrincipal();

        assert userDetails != null;
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .filter(auth -> auth.startsWith("ROLE_"))
                .toList();

        return buildTokenWithDefaultParameters(
                userDetails.getUsername(),
                userDetails.getId().toString(),
                roles
        );
    }

    @Override
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    @Override
    public String generateTokenFromUser(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getRoleName())
                .toList();

        return buildTokenWithDefaultParameters(
                user.getUsername(),
                user.getId().toString(),
                roles
        );
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            LOGGER.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getBearerTokenFrom(HttpServletRequest request){

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
