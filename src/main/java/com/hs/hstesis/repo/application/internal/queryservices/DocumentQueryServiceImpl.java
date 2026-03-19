package com.hs.hstesis.repo.application.internal.queryservices;

import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalIamService;
import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.repo.domain.exceptions.CourseDocumentAccessDeniedException;
import com.hs.hstesis.repo.domain.exceptions.DocumentNotFoundException;
import com.hs.hstesis.repo.domain.exceptions.DocumentWithoutTargetsException;
import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.queries.GetAccessibleDocumentsQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentDownloadQuery;
import com.hs.hstesis.repo.domain.services.DocumentQueryService;
import com.hs.hstesis.repo.domain.services.FileStorageService;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public List<Document> handle(GetAccessibleDocumentsQuery query) {

        Long userId = externalIamService.getAuthenticatedUserId();
        Long courseId = query.courseId();

        boolean isCoordinatorOfCourse = externalLearningService.doesCoordinatorOwnCourse(userId, courseId);

        if (isCoordinatorOfCourse) {
            return documentRepository.findAllByCourseId(courseId);
        }

        var contextOpt = externalLearningService.getUserEnrollmentContextByCourse(userId, courseId);

        if (contextOpt.isEmpty()) {
            throw new CourseDocumentAccessDeniedException(courseId);
        }

        var context = contextOpt.get();

        return documentRepository.findAccessibleDocuments(
                context.courseId(),
                context.educationLevel(),
                context.gradeLevel()
        );
    }

    @Override
    public String handle(GetDocumentDownloadQuery query) {

        Long userId = externalIamService.getAuthenticatedUserId();

        var document = documentRepository.findById(query.documentId())
                .orElseThrow(() -> new DocumentNotFoundException(query.documentId()));

        Long courseId = query.courseId();

        boolean belongsToCourse = document.getTargets().stream()
                .anyMatch(t -> t.getId().getCourseId().equals(courseId));

        if (!belongsToCourse) {
            throw new CourseDocumentAccessDeniedException(courseId);
        }

        boolean isCoordinator = externalLearningService
                .doesCoordinatorOwnCourse(userId, courseId);

        if (!isCoordinator) {
            var contextOpt = externalLearningService
                    .getUserEnrollmentContextByCourse(userId, courseId);

            if (contextOpt.isEmpty()) {
                throw new CourseDocumentAccessDeniedException(courseId);
            }

            var context = contextOpt.get();

            boolean hasAccess = document.getTargets().stream().anyMatch(t ->
                    t.getId().getCourseId().equals(courseId) &&
                            t.getId().getEducationLevel().equals(context.educationLevel()) &&
                            t.getId().getGradeLevel().equals(context.gradeLevel())
            );

            if (!hasAccess) {
                throw new CourseDocumentAccessDeniedException(courseId);
            }
        }

        return fileStorageService.generatePresignedUrl(
                document.getFileStorageInfo().getObjectKey()
        );
    }
}
