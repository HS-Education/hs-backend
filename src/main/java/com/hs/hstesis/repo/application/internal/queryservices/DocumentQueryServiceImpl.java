package com.hs.hstesis.repo.application.internal.queryservices;

import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalIamService;
import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.repo.domain.exceptions.CourseNotFoundException;
import com.hs.hstesis.repo.domain.exceptions.DocumentNotFoundException;
import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.queries.GetAccessibleDocumentsQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentByIdQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentDownloadQuery;
import com.hs.hstesis.repo.domain.services.DocumentQueryService;
import com.hs.hstesis.repo.domain.services.FileStorageService;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentQueryServiceImpl implements DocumentQueryService {
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final ExternalLearningService externalLearningService;
    private final ExternalIamService  externalIamService;

    public  DocumentQueryServiceImpl(DocumentRepository documentRepository,
                                     FileStorageService fileStorageService,
                                     ExternalLearningService externalLearningService,
                                     ExternalIamService externalIamService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.externalLearningService = externalLearningService;
        this.externalIamService = externalIamService;
    }

    @Override
    public Optional<Document> handle(GetDocumentByIdQuery query) {
        return documentRepository.findById(query.documentId());
    }

    @Override
    public List<Document> handle(GetAccessibleDocumentsQuery query) {

        Long userId = externalIamService.getAuthenticatedUserId();
        Long courseId = query.courseId();

        boolean courseExists = externalLearningService.existsCourse(courseId);
        boolean isCoordinatorOfCourse = externalLearningService.doesCoordinatorOwnCourse(userId, courseId);

        if (isCoordinatorOfCourse) {
            return documentRepository.findAllByCourseId(courseId);
        }

        var contextOpt = externalLearningService.getUserEnrollmentContextByCourse(userId, courseId);

        if (!courseExists || (!isCoordinatorOfCourse && contextOpt.isEmpty())) {
            throw new CourseNotFoundException(courseId);
        }

        var context = contextOpt.get();

        return documentRepository.findAccessibleDocuments(
                context.courseId(),
                context.educationLevel(),
                context.gradeLevel()
        );
    }

    @Transactional(readOnly = true)
    @Override
    public String handle(GetDocumentDownloadQuery query) {

        Long documentId = query.documentId();

        var document = documentRepository.findByIdWithTargets(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        Long courseId = query.courseId();

        boolean belongsToCourse = document.getTargets().stream()
                .anyMatch(t -> t.getId().getCourseId().equals(courseId));

        if (!belongsToCourse) {
            throw new DocumentNotFoundException(documentId);
        }

        Long userId = externalIamService.getAuthenticatedUserId();

        boolean isCoordinator = externalLearningService.doesCoordinatorOwnCourse(userId, courseId);

        if (!isCoordinator) {
            var contextOpt = externalLearningService.getUserEnrollmentContextByCourse(userId, courseId);

            if (contextOpt.isEmpty()) {
                throw new DocumentNotFoundException(documentId);
            }

            var context = contextOpt.get();

            boolean hasAccess = document.getTargets().stream().anyMatch(t ->
                    t.getId().getCourseId().equals(courseId) &&
                            t.getId().getEducationLevel().equals(context.educationLevel()) &&
                            t.getId().getGradeLevel().equals(context.gradeLevel())
            );

            if (!hasAccess) {
                throw new DocumentNotFoundException(documentId);
            }
        }

        return fileStorageService.generatePresignedUrl(
                document.getFileStorageInfo().getObjectKey()
        );
    }
}
