package com.hs.hstesis.repo.domain.model.commands;

public record DeleteDocumentCommand(Long courseId, Long documentId) {
    public DeleteDocumentCommand {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course id cannot be null or negative.");
        }
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Document id cannot be null or negative.");
        }
    }
}
