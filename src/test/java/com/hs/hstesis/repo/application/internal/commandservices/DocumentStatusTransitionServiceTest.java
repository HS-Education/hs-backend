package com.hs.hstesis.repo.application.internal.commandservices;

import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentFormat;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentStatus;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentType;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentStatusTransitionServiceTest {
    @Test
    void processingFailureMarksDocumentFailedAndPersistsTheTransition() {
        var repository = mock(DocumentRepository.class);
        var document = new Document("Synthetic lesson", 1L, 2L, DocumentType.ACADEMIC,
                DocumentFormat.PDF, "lesson.pdf", "documents/2/lesson.pdf", "checksum");
        document.markAsProcessing();
        when(repository.findByIdWithTargetsForUpdate(73L)).thenReturn(Optional.of(document));

        new DocumentStatusTransitionService(repository).markFailed(73L);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        verify(repository).save(document);
    }
}
