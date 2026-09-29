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

    @Transactional
    @Override
    public java.util.List<Long> handle(com.hs.hstesis.repo.domain.model.commands.UploadBulkDocumentsCommand command, java.util.List<UploadFile> uploadFiles) {
        if (command.documentsMetadata().size() != uploadFiles.size()) {
            throw new IllegalArgumentException("Number of files must match number of metadata entries.");
        }

        var requestedDocumentsByTopic = command.documentsMetadata().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        com.hs.hstesis.repo.interfaces.rest.resources.UploadBulkDocumentMetadataResource::topicId,
                        java.util.stream.Collectors.counting()));

        for (var entry : requestedDocumentsByTopic.entrySet()) {
            long existingDocuments = documentRepository.countByTopicId(entry.getKey());
            long requestedDocuments = entry.getValue();
            if (existingDocuments + requestedDocuments > 3) {
                throw new IllegalArgumentException(
                        "A topic can have a maximum of 3 documents. Topic " + entry.getKey()
                                + " already has " + existingDocuments + " document(s).");
            }
        }

        java.util.List<Long> documentIds = new java.util.ArrayList<>();

        for (int i = 0; i < uploadFiles.size(); i++) {
            var metadata = command.documentsMetadata().get(i);
            var file = uploadFiles.get(i);
            
            var uploadDocCommand = new UploadDocumentCommand(
                    metadata.title(),
                    metadata.topicId(),
                    com.hs.hstesis.repo.domain.model.valueobjects.DocumentType.ACADEMIC,
                    com.hs.hstesis.repo.domain.model.valueobjects.DocumentFormat.fromFileName(metadata.fileName()),
                    metadata.fileName(),
                    com.hs.hstesis.repo.domain.model.valueobjects.EducationLevel.valueOf(metadata.educationLevel()),
                    metadata.gradeLevels().stream().map(com.hs.hstesis.repo.domain.model.valueobjects.GradeLevel::valueOf).toList(),
                    command.courseId()
            );

            Long documentId = this.handle(uploadDocCommand, file);
            documentIds.add(documentId);
        }

        return documentIds;
    }
}
