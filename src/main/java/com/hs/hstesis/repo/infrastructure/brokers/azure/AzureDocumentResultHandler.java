package com.hs.hstesis.repo.infrastructure.brokers.azure;

import com.hs.hstesis.repo.domain.model.commands.SaveDocumentEmbeddingsCommand;
import com.hs.hstesis.repo.domain.model.valueobjects.ChunkEmbeddingData;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentStatus;
import com.hs.hstesis.repo.domain.services.DocumentCommandService;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.messaging.provider", havingValue = "service-bus")
public class AzureDocumentResultHandler {
    public record Failure(int schemaVersion, long documentId, int generation, String errorCode) {
        public Failure {
            if (schemaVersion != 1 || documentId < 1 || generation < 1 || errorCode == null
                    || !java.util.Set.of("DOCUMENT_INVALID", "PROCESSING_UNAVAILABLE").contains(errorCode)) {
                throw new IllegalArgumentException("Invalid processing failure");
            }
        }
    }
    private final DocumentRepository documents;
    private final DocumentCommandService commands;
    private final EmbeddingsResultStore results;
    public AzureDocumentResultHandler(DocumentRepository documents, DocumentCommandService commands, EmbeddingsResultStore results) {
        this.documents = documents; this.commands = commands; this.results = results;
    }
    @Transactional
    public void accept(EmbeddingsReference reference) {
        var document = documents.findByIdWithTargetsForUpdate(reference.documentId()).orElse(null);
        if (document == null || document.getProcessingGeneration() != reference.generation()
                || document.getStatus() == DocumentStatus.READY || document.getStatus() == DocumentStatus.FAILED) return;
        var payload = results.read(reference);
        payload.validateAgainst(reference);
        commands.handle(new SaveDocumentEmbeddingsCommand(reference.documentId(), payload.chunks().stream()
                .map(c -> new ChunkEmbeddingData(c.pageNumber(), c.chunkIndex(), c.content(), c.embedding())).toList()));
        // The result blob is retained for retries and expires later via its lifecycle policy.
    }
    @Transactional
    public void fail(Failure failure) {
        documents.findByIdWithTargetsForUpdate(failure.documentId()).ifPresent(document -> {
            if (document.getProcessingGeneration() == failure.generation()) {
                document.markAsFailed();
                documents.save(document);
            }
        });
    }
}
