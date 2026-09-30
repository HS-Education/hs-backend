package com.hs.hstesis.repo.infrastructure.storage.azure;

import com.azure.storage.blob.BlobServiceClient;
import com.hs.hstesis.repo.infrastructure.brokers.azure.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "azure-blob")
public class BlobEmbeddingsResultStore implements EmbeddingsResultStore {
    private final BlobServiceClient storage;
    private final String container;
    public BlobEmbeddingsResultStore(BlobServiceClient storage,
                                     @Value("${azure.storage.results-container}") String container) {
        this.storage = storage;
        this.container = container;
    }
    @Override
    public EmbeddingsPayload read(EmbeddingsReference reference) {
        var blob = storage.getBlobContainerClient(container).getBlobClient(reference.objectKey());
        if (blob.getProperties().getBlobSize() != reference.sizeBytes()) {
            throw new IllegalArgumentException("Embeddings blob size mismatch");
        }
        try (InputStream stream = blob.openInputStream()) {
            byte[] data = stream.readNBytes(Math.toIntExact(reference.sizeBytes()) + 1);
            if (data.length != reference.sizeBytes() || !reference.sha256().equals(
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)))) {
                throw new IllegalArgumentException("Embeddings blob integrity mismatch");
            }
            var result = AzureJson.read(data, EmbeddingsPayload.class);
            result.validateAgainst(reference);
            return result;
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not read embeddings result", e);
        }
    }
}
