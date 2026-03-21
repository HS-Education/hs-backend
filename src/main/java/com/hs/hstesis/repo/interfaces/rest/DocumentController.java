package com.hs.hstesis.repo.interfaces.rest;

import com.hs.hstesis.repo.domain.model.commands.UploadDocumentCommand;
import com.hs.hstesis.repo.domain.model.queries.GetAccessibleDocumentsQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentByIdQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentDownloadQuery;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentFormat;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentType;
import com.hs.hstesis.repo.domain.services.DocumentCommandService;
import com.hs.hstesis.repo.domain.services.DocumentQueryService;
import com.hs.hstesis.repo.interfaces.rest.resources.DocumentResource;
import com.hs.hstesis.repo.interfaces.rest.resources.DownloadDocumentResource;
import com.hs.hstesis.repo.interfaces.rest.resources.UploadDocumentResource;
import com.hs.hstesis.repo.interfaces.rest.transform.DocumentResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/courses/{courseId}/documents", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Documents", description = "Document management endpoints")
public class DocumentController {
    private final DocumentCommandService documentCommandService;
    private final DocumentQueryService documentQueryService;

    public DocumentController(DocumentCommandService documentCommandService,
                              DocumentQueryService documentQueryService) {
        this.documentCommandService = documentCommandService;
        this.documentQueryService = documentQueryService;
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(description = "Uploads a new document to the repository.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResource> uploadDocument(
            @PathVariable Long courseId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") UploadDocumentResource resource) {

        var uploadDocumentCommand = new UploadDocumentCommand(
                resource.title(),
                resource.topicId(),
                DocumentType.ACADEMIC,
                DocumentFormat.fromFileName(file.getOriginalFilename()),
                file.getOriginalFilename(),
                resource.educationLevel(),
                resource.gradeLevels(),
                courseId
        );

        var documentId = documentCommandService.handle(uploadDocumentCommand, file);

        var getDocumentByIdQuery = new GetDocumentByIdQuery(documentId);
        var document = documentQueryService.handle(getDocumentByIdQuery);

        if (document.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var documentResource = DocumentResourceFromEntityAssembler.toResourceFromEntity(document.get());
        return new ResponseEntity<>(documentResource, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('REPOSITORY_READ')")
    @Operation(description = "Retrieves a list of documents accessible to the authenticated user for the specified course.")
    @GetMapping
    public ResponseEntity<List<DocumentResource>> getAccessibleDocumentsByCourse(@PathVariable Long courseId) {
        var getAccessibleDocumentsQuery = new GetAccessibleDocumentsQuery(courseId);

        var documents = documentQueryService.handle(getAccessibleDocumentsQuery);

        var documentsResources = documents.stream()
                .map(DocumentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(documentsResources);
    }

    @PreAuthorize("hasAuthority('REPOSITORY_READ')")
    @Operation(description = "Retrieves a download URL for the specified document if the authenticated user has access to it.")
    @GetMapping("/{documentId}/download")
    public ResponseEntity<DownloadDocumentResource> downloadDocument(@PathVariable Long courseId, @PathVariable Long documentId) {

        var query = new GetDocumentDownloadQuery(documentId, courseId);
        String url = documentQueryService.handle(query);

        return ResponseEntity.ok(new DownloadDocumentResource(url));
    }

}
