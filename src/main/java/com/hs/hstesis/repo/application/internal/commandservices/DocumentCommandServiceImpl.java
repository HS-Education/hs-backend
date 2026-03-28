package com.hs.hstesis.repo.application.internal.commandservices;

import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalIamService;
import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.repo.application.internal.outboundservices.storage.UploadFile;
import com.hs.hstesis.repo.domain.exceptions.*;
import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.commands.DeleteDocumentCommand;
import com.hs.hstesis.repo.domain.model.commands.SaveDocumentEmbeddingsCommand;
import com.hs.hstesis.repo.domain.model.commands.UploadDocumentCommand;
import com.hs.hstesis.repo.domain.services.DocumentCommandService;
import com.hs.hstesis.repo.application.internal.outboundservices.storage.FileStorageService;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentCommandServiceImpl implements DocumentCommandService {
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final ExternalIamService externalIamService;
    private final ExternalLearningService externalLearningService;

    public DocumentCommandServiceImpl(DocumentRepository documentRepository,
                                      FileStorageService fileStorageService,
                                      ExternalIamService externalIamService,
                                      ExternalLearningService externalLearningService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.externalIamService = externalIamService;
        this.externalLearningService = externalLearningService;
    }

    @Transactional
    @Override
    public Long handle(UploadDocumentCommand command, UploadFile file) {
        if (file.size() <= 0) {
            throw new UploadedFileIsEmptyException();
        }

        Long userId = externalIamService.getAuthenticatedUserId();

        boolean isCoordinatorAssignedToAnyArea = externalLearningService.isCoordinatorAssignedToAnyArea(userId);
        if (!isCoordinatorAssignedToAnyArea) {
            throw new CoordinatorNotAssignedToAnyAreaException();
        }

        boolean isCoordinatorCourse = externalLearningService
                .doesCoordinatorOwnCourse(userId, command.courseId());
        if (!isCoordinatorCourse) {
            throw new CoordinatorDoesNotOwnCourseException();
        }

        boolean topicBelongsToCourse = externalLearningService
                .doesTopicBelongToCourse(command.topicId(), command.courseId());
        if (!topicBelongsToCourse) {
            throw new TopicDoesNotBelongToCourseException(command.topicId(), command.courseId());
        }

        String checksum = fileStorageService.calculateChecksum(file);

        var existing = documentRepository.findByChecksum(checksum);
        if (existing.isPresent()) {
            throw new DocumentAlreadyExistsException(checksum);
        }

        String objectKey = fileStorageService.generateObjectKey(command.originalFileName(), command.topicId());
        fileStorageService.upload(file, objectKey);

        var document = new Document(
                command.title(),
                userId,
                command.topicId(),
                command.type(),
                command.format(),
                command.originalFileName(),
                objectKey,
                checksum
        );

        document.addTargets(
                command.educationLevel(),
                command.gradeLevels(),
                command.courseId()
        );

        try {
            documentRepository.saveAndFlush(document);
            document.confirmUpload();
            documentRepository.save(document);
            return document.getId();

        } catch (DataIntegrityViolationException e) {
            try {
                fileStorageService.delete(objectKey);
            } catch (RuntimeException deleteEx) {
                // Log the error but don't rethrow, since we want to return the existing document ID if possible
            }

            return documentRepository.findByChecksum(checksum)
                    .map(Document::getId)
                    .orElseThrow(() -> new DocumentDeduplicationStateException(checksum, e));
        }
    }

    @Transactional
    @Override
    public void handle(DeleteDocumentCommand command) {

        var document = documentRepository.findById(command.documentId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Long courseId = command.courseId();

        boolean belongsToCourse = document.getTargets().stream()
                .anyMatch(t -> t.getId().getCourseId().equals(courseId));

        if (!belongsToCourse) {
            throw new CourseDocumentAccessDeniedException(courseId);
        }

        Long userId = externalIamService.getAuthenticatedUserId();

        boolean isCoordinator = externalLearningService
                .doesCoordinatorOwnCourse(userId, courseId);

        if (!isCoordinator) {
            throw new CourseDocumentAccessDeniedException(courseId);
        }

        String objectKey = document.getFileStorageInfo().getObjectKey();

        try {
            fileStorageService.delete(objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting file from storage", e);
        }

        documentRepository.delete(document);
    }

    @Transactional
    @Override
    public void handle(SaveDocumentEmbeddingsCommand command) {
        var document = documentRepository.findById(command.documentId())
                .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));

        document.replaceChunks(command.chunks());
        document.markAsReady();

        documentRepository.save(document);
    }
}
