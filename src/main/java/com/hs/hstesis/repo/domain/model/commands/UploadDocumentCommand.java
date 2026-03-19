package com.hs.hstesis.repo.domain.model.commands;

import com.hs.hstesis.repo.domain.model.valueobjects.DocumentFormat;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentType;
import com.hs.hstesis.repo.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.GradeLevel;

import java.util.List;

public record UploadDocumentCommand(
        String title,
        Long topicId,
        DocumentType type,
        DocumentFormat format,
        String originalFileName,
        String objectKey,
        String fileChecksum,
        EducationLevel educationLevel,
        List<GradeLevel> gradeLevels,
        Long courseId
) {
    public UploadDocumentCommand {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
        if (topicId == null || topicId <= 0) {
            throw new IllegalArgumentException("Topic id cannot be null or negative.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Document type cannot be null");
        }
        if (format == null) {
            throw new IllegalArgumentException("Document format cannot be null");
        }
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("Original file name cannot be null or blank");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key cannot be null or blank");
        }
        if (fileChecksum == null || fileChecksum.isBlank()) {
            throw new IllegalArgumentException("File checksum cannot be null or blank");
        }
        if (educationLevel == null) {
            throw new IllegalArgumentException("Education level cannot be null.");
        }
        if (gradeLevels == null || gradeLevels.isEmpty()) {
            throw new IllegalArgumentException("Grades cannot be null.");
        }
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
    }
}
