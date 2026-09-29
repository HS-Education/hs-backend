package com.hs.hstesis.repo.interfaces.rest.resources;

import com.hs.hstesis.repo.domain.model.valueobjects.DocumentFormat;

public record DocumentResource(Long id,
                               String title,
                               Long topicId,
                               DocumentFormat format,
                               String originalFileName,
                               java.util.Date createdAt) {}
