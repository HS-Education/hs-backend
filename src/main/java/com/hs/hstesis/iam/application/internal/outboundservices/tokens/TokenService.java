package com.hs.hstesis.iam.application.internal.outboundservices.tokens;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface TokenService {

    String generateToken(Authentication authentication);
    String generateTokenFromUser(User user);
    String getUsernameFromToken(String token);
    boolean validateToken(String token);
}