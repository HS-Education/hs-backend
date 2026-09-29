package com.hs.hstesis.repo.interfaces.rest.resources;

import com.hs.hstesis.repo.domain.model.valueobjects.DocumentFormat;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentStatus;

public record DocumentResource(Long id,
                               String title,
                               Long topicId,
                               DocumentFormat format,
                               String originalFileName,
                               java.util.Date createdAt,
                               @com.fasterxml.jackson.annotation.JsonProperty("document_status") DocumentStatus status) {}
