package com.hs.hstesis.repo.infrastructure.brokers.azure;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class AzureJson {
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder().streamReadConstraints(
            StreamReadConstraints.builder().maxNestingDepth(16).maxStringLength(1_000_000).build()).build());
    private AzureJson() {}
    public static String write(Object value) {
        try { return MAPPER.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid message payload", e); }
    }
    public static <T> T read(byte[] json, Class<T> type) {
        try { return MAPPER.readValue(json, type); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid message payload", e); }
    }
}
