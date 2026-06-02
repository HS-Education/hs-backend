package com.hs.hstesis.repo.infrastructure.outboundservices.ai;

import java.util.List;

public record EmbedQueryResponse(String model, int dimensions, List<Float> embedding) {}
