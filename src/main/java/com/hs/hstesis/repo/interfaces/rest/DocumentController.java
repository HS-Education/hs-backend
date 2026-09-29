package com.hs.hstesis.repo.interfaces.rest;

import com.hs.hstesis.repo.domain.model.commands.DeleteDocumentCommand;
import com.hs.hstesis.repo.domain.model.queries.GetAccessibleDocumentsQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentByIdQuery;
import com.hs.hstesis.repo.domain.model.queries.GetDocumentDownloadQuery;
import com.hs.hstesis.repo.domain.services.DocumentCommandService;
import com.hs.hstesis.repo.domain.services.DocumentQueryService;
import com.hs.hstesis.repo.interfaces.rest.adapters.UploadFileFromMultipartAdapter;
import com.hs.hstesis.repo.interfaces.rest.resources.DocumentResource;
import com.hs.hstesis.repo.interfaces.rest.resources.DownloadDocumentResource;
import com.hs.hstesis.repo.interfaces.rest.transform.DocumentResourceFromEntityAssembler;
import com.hs.hstesis.shared.interfaces.rest.resources.MessageResource;
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
    @Operation(description = "Uploads multiple documents to the repository for a given bimester.")
    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<DocumentResource>> uploadBulkDocuments(
            @PathVariable Long courseId,
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("data") String dataJson) {

        com.hs.hstesis.repo.interfaces.rest.resources.UploadBulkDocumentsResource resource;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            resource = mapper.readValue(dataJson, com.hs.hstesis.repo.interfaces.rest.resources.UploadBulkDocumentsResource.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Jackson's diagnostic can include excerpts of user-supplied content.
            throw new IllegalArgumentException("Invalid JSON format in 'data' field.");
        }

        var uploadBulkCommand = new com.hs.hstesis.repo.domain.model.commands.UploadBulkDocumentsCommand(
                resource.bimester(),
                resource.documents(),
                courseId
        );

        List<com.hs.hstesis.repo.application.internal.outboundservices.storage.UploadFile> uploadFiles = files.stream()
                .map(f -> (com.hs.hstesis.repo.application.internal.outboundservices.storage.UploadFile) new UploadFileFromMultipartAdapter(f))
                .toList();

        var documentIds = documentCommandService.handle(uploadBulkCommand, uploadFiles);

        List<DocumentResource> documentResources = documentIds.stream()
                .map(id -> documentQueryService.handle(new GetDocumentByIdQuery(id)))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(DocumentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return new ResponseEntity<>(documentResources, HttpStatus.CREATED);
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

    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(description = "Deletes a document from the repository.")
    @DeleteMapping("/{documentId}")
    public ResponseEntity<MessageResource> deleteDocument(@PathVariable Long courseId, @PathVariable Long documentId) {
        var deleteDocumentCommand = new DeleteDocumentCommand(courseId, documentId);
        documentCommandService.handle(deleteDocumentCommand);
        return ResponseEntity.ok(new MessageResource("Document deleted successfully"));
    }

    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(description = "Retries AI processing for a failed document owned by the authenticated coordinator.")
    @PostMapping("/{documentId}/retry-processing")
    public ResponseEntity<Void> retryDocumentProcessing(@PathVariable Long courseId, @PathVariable Long documentId) {
        documentCommandService.retryProcessing(new com.hs.hstesis.repo.domain.model.commands.RetryDocumentProcessingCommand(
                courseId, documentId));
        return ResponseEntity.accepted().build();
    }

}
