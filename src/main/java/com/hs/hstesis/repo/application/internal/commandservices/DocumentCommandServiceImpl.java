package com.hs.hstesis.repo.application.internal.commandservices;

import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalIamService;
import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.repo.domain.exceptions.CoordinatorDoesNotOwnCourseException;
import com.hs.hstesis.repo.domain.exceptions.CoordinatorNotAssignedToAnyAreaException;
import com.hs.hstesis.repo.domain.exceptions.TopicDoesNotBelongToCourseException;
import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.commands.UploadDocumentCommand;
import com.hs.hstesis.repo.domain.services.DocumentCommandService;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import org.springframework.stereotype.Service;

@Service
public class DocumentCommandServiceImpl implements DocumentCommandService {
    private final DocumentRepository documentRepository;
    private final ExternalIamService externalIamService;
    private final ExternalLearningService externalLearningService;

    public DocumentCommandServiceImpl(DocumentRepository documentRepository,
                                      ExternalIamService externalIamService,
                                      ExternalLearningService externalLearningService) {
        this.documentRepository = documentRepository;
        this.externalIamService = externalIamService;
        this.externalLearningService = externalLearningService;
    }

    @Override
    public Long handle(UploadDocumentCommand command) {
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

        var document = new Document(
                command.title(),
                userId,
                command.topicId(),
                command.type(),
                command.format(),
                command.originalFileName(),
                command.objectKey(),
                command.fileChecksum()
        );

        document.addTargets(
                command.educationLevel(),
                command.gradeLevels(),
                command.courseId()
        );
        documentRepository.save(document);
        return document.getId();
    }

}
