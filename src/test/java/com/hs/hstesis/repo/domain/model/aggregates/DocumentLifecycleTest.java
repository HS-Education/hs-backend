package com.hs.hstesis.repo.domain.model.aggregates;

import com.hs.hstesis.repo.domain.exceptions.InvalidDocumentStatusTransitionException;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentFormat;
import com.hs.hstesis.repo.domain.model.valueobjects.ChunkEmbeddingData;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentStatus;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentType;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentLifecycleTest {
    private static Document document() {
        return new Document("Synthetic lesson", 1L, 2L, DocumentType.ACADEMIC,
                DocumentFormat.PDF, "lesson.pdf", "documents/2/lesson.pdf", "checksum");
    }

    @Test
    void failedDocumentCannotBecomeReady() {
        Document document = document();
        document.markAsProcessing();
        document.markAsFailed();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThatThrownBy(document::markAsReady).isInstanceOf(InvalidDocumentStatusTransitionException.class);
    }

    @Test
    void duplicateAndLateFailureEventsDoNotDowngradeReadyDocument() {
        Document failed = document();
        failed.markAsFailed();
        failed.markAsFailed();
        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);

        Document ready = document();
        ready.markAsProcessing();
        ready.replaceChunks(List.of(new ChunkEmbeddingData(1, 0, "Synthetic academic source", new float[1024])));
        ready.markAsReady();
        ready.markAsFailed();
        assertThat(ready.getStatus()).isEqualTo(DocumentStatus.READY);
    }
}
