package com.hs.hstesis.repo.infrastructure.outboundservices.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GenerateRequest(
        List<AiMessageDto> messages,
        @JsonProperty("context_chunks") List<String> contextChunks,
        @JsonProperty("max_tokens") int maxTokens,
        float temperature,
        boolean stream
) {}
