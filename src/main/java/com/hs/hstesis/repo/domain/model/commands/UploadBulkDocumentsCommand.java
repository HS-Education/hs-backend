package com.hs.hstesis.repo.domain.model.commands;

import com.hs.hstesis.repo.interfaces.rest.resources.UploadBulkDocumentMetadataResource;
import java.util.List;

public record UploadBulkDocumentsCommand(
        String bimester,
        List<UploadBulkDocumentMetadataResource> documentsMetadata,
        Long courseId
) {
}
