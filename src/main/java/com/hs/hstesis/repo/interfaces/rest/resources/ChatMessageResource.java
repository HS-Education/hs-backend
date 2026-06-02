package com.hs.hstesis.repo.interfaces.rest.resources;

import java.time.LocalDateTime;

public record ChatMessageResource(
        Long id,
        String role,
        String content,
        LocalDateTime createdAt
) {}
