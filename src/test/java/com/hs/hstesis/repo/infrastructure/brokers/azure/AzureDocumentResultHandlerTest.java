package com.hs.hstesis.repo.infrastructure.brokers.azure;

import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentStatus;
import com.hs.hstesis.repo.domain.services.DocumentCommandService;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos.ChunkVector;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;

class AzureDocumentResultHandlerTest {
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DocumentCommandService commands = mock(DocumentCommandService.class);
    private final EmbeddingsResultStore store = mock(EmbeddingsResultStore.class);
    private final AzureDocumentResultHandler handler = new AzureDocumentResultHandler(documents, commands, store);
    private EmbeddingsReference reference() { String hash = "a".repeat(64); return new EmbeddingsReference(1, 2, 3,
            "processing-results/2/3/" + hash + ".json", hash, 2000, 1, 1024); }
    @Test void staleResultCannotOverwriteANewerGeneration() {
        var document = mock(Document.class);
        when(document.getProcessingGeneration()).thenReturn(4);
        when(documents.findByIdWithTargetsForUpdate(2L)).thenReturn(Optional.of(document));
        handler.accept(reference());
        verifyNoInteractions(store, commands);
    }
    @Test void duplicateReadyResultIsIgnored() {
        var document = mock(Document.class);
        when(document.getProcessingGeneration()).thenReturn(3);
        when(document.getStatus()).thenReturn(DocumentStatus.READY);
        when(documents.findByIdWithTargetsForUpdate(2L)).thenReturn(Optional.of(document));
        handler.accept(reference());
        verifyNoInteractions(store, commands);
    }
    @Test void validProcessingResultIsPersisted() {
        var document = mock(Document.class);
        when(document.getProcessingGeneration()).thenReturn(3);
        when(document.getStatus()).thenReturn(DocumentStatus.PROCESSING);
        when(documents.findByIdWithTargetsForUpdate(2L)).thenReturn(Optional.of(document));
        when(store.read(reference())).thenReturn(new EmbeddingsPayload(2, 3,
                List.of(new ChunkVector(1, 0, "lesson", new float[1024]))));
        handler.accept(reference());
        verify(commands).handle(any(com.hs.hstesis.repo.domain.model.commands.SaveDocumentEmbeddingsCommand.class));
    }
    @Test void staleFailureCannotFailANewerGeneration() {
        var document = mock(Document.class);
        when(document.getProcessingGeneration()).thenReturn(4);
        when(documents.findByIdWithTargetsForUpdate(2L)).thenReturn(Optional.of(document));
        handler.fail(new AzureDocumentResultHandler.Failure(1, 2, 3, "DOCUMENT_INVALID"));
        verify(document, never()).markAsFailed();
        verify(documents, never()).save(any());
    }
}
