package com.hs.hstesis.repo.infrastructure.outboundservices.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EmbedQueryResponse(
    @JsonProperty("model") String model,
    @JsonProperty("dimensions") int dimensions,
    @JsonProperty("embedding") List<Float> embedding
) {}
