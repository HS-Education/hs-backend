package com.hs.hstesis.repo.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UploadBulkDocumentMetadataResource(
        @NotBlank String fileName,
        @NotBlank String title,
        @NotNull Long topicId,
        @NotNull String educationLevel,
        List<String> gradeLevels
) {
}
