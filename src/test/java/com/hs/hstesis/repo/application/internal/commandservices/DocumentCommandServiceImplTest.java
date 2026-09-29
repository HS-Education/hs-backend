package com.hs.hstesis.repo.application.internal.commandservices;

import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalIamService;
import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.repo.application.internal.outboundservices.storage.FileStorageService;
import com.hs.hstesis.repo.application.internal.outboundservices.storage.UploadFile;
import com.hs.hstesis.repo.application.internal.validation.PdfUploadValidator;
import com.hs.hstesis.repo.domain.exceptions.CoordinatorDoesNotOwnCourseException;
import com.hs.hstesis.repo.domain.exceptions.InvalidPdfUploadException;
import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.commands.SaveDocumentEmbeddingsCommand;
import com.hs.hstesis.repo.domain.model.commands.UploadBulkDocumentsCommand;
import com.hs.hstesis.repo.domain.model.commands.RetryDocumentProcessingCommand;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentFormat;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentStatus;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentType;
import com.hs.hstesis.repo.domain.model.valueobjects.ChunkEmbeddingData;
import com.hs.hstesis.repo.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.GradeLevel;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import com.hs.hstesis.repo.interfaces.rest.resources.UploadBulkDocumentMetadataResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DocumentCommandServiceImplTest {
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final ExternalIamService iam = mock(ExternalIamService.class);
    private final ExternalLearningService learning = mock(ExternalLearningService.class);
    private final PdfUploadValidator validator = mock(PdfUploadValidator.class);
    private final DocumentCommandServiceImpl service =
            new DocumentCommandServiceImpl(documents, storage, iam, learning, validator);

    private UploadBulkDocumentsCommand command() {
        return new UploadBulkDocumentsCommand("FIRST", List.of(
                new UploadBulkDocumentMetadataResource("one.pdf", "One", 11L, "SECONDARY", List.of("SECOND")),
                new UploadBulkDocumentMetadataResource("two.pdf", "Two", 12L, "SECONDARY", List.of("SECOND"))
        ), 10L);
    }

    @BeforeEach
    void user() {
        when(iam.getAuthenticatedUserId()).thenReturn(7L);
    }

    @Test
    void ownershipIsCheckedBeforeParsingAnyPdf() {
        when(learning.isCoordinatorAssignedToAnyArea(7L)).thenReturn(true);
        UploadFile first = mock(UploadFile.class);
        UploadFile second = mock(UploadFile.class);
        assertThatThrownBy(() -> service.handle(command(), List.of(first, second)))
                .isInstanceOf(CoordinatorDoesNotOwnCourseException.class);
        verifyNoInteractions(validator, storage);
    }

    @Test
    void invalidSecondPdfRejectsWholeBatchBeforeStorage() {
        when(learning.isCoordinatorAssignedToAnyArea(7L)).thenReturn(true);
        when(learning.doesCoordinatorOwnCourse(7L, 10L)).thenReturn(true);
        when(learning.doesTopicBelongToCourse(anyLong(), eq(10L))).thenReturn(true);
        UploadFile first = mock(UploadFile.class);
        UploadFile second = mock(UploadFile.class);
        when(validator.validate(first, "one.pdf")).thenReturn(first);
        when(validator.validate(second, "two.pdf")).thenThrow(new InvalidPdfUploadException());

        assertThatThrownBy(() -> service.handle(command(), List.of(first, second)))
                .isInstanceOf(InvalidPdfUploadException.class);
        verify(storage, never()).upload(any(), anyString());
        verify(documents, never()).saveAndFlush(any());
    }

    @Test
    void lateEmbeddingResultDoesNotReviveFailedDocument() {
        var document = new Document("Lesson", 7L, 11L, DocumentType.ACADEMIC,
                DocumentFormat.PDF, "one.pdf", "object-1", "checksum-1");
        document.markAsFailed();
        when(documents.findByIdWithTargetsForUpdate(1L)).thenReturn(Optional.of(document));

        service.handle(new SaveDocumentEmbeddingsCommand(1L,
                List.of(new ChunkEmbeddingData(1, 0, "Lesson text", new float[]{1f}))));

        verify(documents, never()).save(any());
        org.assertj.core.api.Assertions.assertThat(document.getStatus().name()).isEqualTo("FAILED");
    }

    @Test
    void retryingFailedDocumentSavesAggregateToPublishProcessingEvent() {
        var document = new Document("Lesson", 7L, 11L, DocumentType.ACADEMIC,
                DocumentFormat.PDF, "one.pdf", "object-1", "checksum-1");
        document.addTargets(EducationLevel.SECONDARY, List.of(GradeLevel.SECOND), 10L);
        document.markAsFailed();
        when(documents.findByIdWithTargetsForUpdate(5L)).thenReturn(Optional.of(document));
        when(learning.doesCoordinatorOwnCourse(7L, 10L)).thenReturn(true);

        service.retryProcessing(new RetryDocumentProcessingCommand(10L, 5L));

        org.assertj.core.api.Assertions.assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        verify(documents, times(1)).save(document);
    }
}
