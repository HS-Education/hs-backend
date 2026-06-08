package com.hs.hstesis.repo.interfaces.rest.resources;

public record ChatSessionResource(
        Long id,
        Long courseId,
        Long userId,
        Integer sessionNumber
) {}
