package com.hs.hstesis.iam.infrastructure.tokens.jwt;

import com.hs.hstesis.iam.domain.model.entity.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId);
    RefreshToken verifyExpiration(RefreshToken token);
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserId(Long userId);
    String getRefreshTokenFromCookie(HttpServletRequest request);
}
