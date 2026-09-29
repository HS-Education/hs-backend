package com.hs.hstesis.repo.infrastructure.outboundservices.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmbedQueryRequest(
    @JsonProperty("text") String text
) {}
