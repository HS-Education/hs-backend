package com.hs.hstesis.repo.infrastructure.brokers.azure;

public interface EmbeddingsResultStore {
    EmbeddingsPayload read(EmbeddingsReference reference);
}
