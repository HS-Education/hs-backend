package com.hs.hstesis.iam.infrastructure.hashing.bcrypt;

import com.hs.hstesis.iam.application.internal.outboundservices.hashing.HashingService;
import org.springframework.security.crypto.password.PasswordEncoder;

public interface BCryptHashingService extends HashingService, PasswordEncoder {}
