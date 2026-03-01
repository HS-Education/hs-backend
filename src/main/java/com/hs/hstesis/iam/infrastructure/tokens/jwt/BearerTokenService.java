package com.hs.hstesis.iam.infrastructure.tokens.jwt;

import com.hs.hstesis.iam.application.internal.outboundservices.tokens.TokenService;
import jakarta.servlet.http.HttpServletRequest;

public interface BearerTokenService extends TokenService {

    String getBearerTokenFrom(HttpServletRequest request);
}