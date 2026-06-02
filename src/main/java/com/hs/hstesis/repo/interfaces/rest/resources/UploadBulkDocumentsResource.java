package com.hs.hstesis.repo.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UploadBulkDocumentsResource(
        @NotBlank String bimester,
        @NotEmpty List<UploadBulkDocumentMetadataResource> documents
) {
}
