package com.hs.hstesis.repo.domain.model.aggregates;

import com.hs.hstesis.repo.domain.exceptions.DocumentChunksRequiredException;
import com.hs.hstesis.repo.domain.exceptions.InvalidDocumentStatusTransitionException;
import com.hs.hstesis.repo.domain.model.entities.DocumentChunk;
import com.hs.hstesis.repo.domain.model.entities.DocumentTarget;
import com.hs.hstesis.repo.domain.model.events.DocumentUploadedEvent;
import com.hs.hstesis.repo.domain.model.valueobjects.*;
import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Table(name = "documents",
        indexes = {
                @Index(name = "idx_documents_topic", columnList = "topic_id"),
                @Index(name = "idx_documents_author", columnList = "author_id"),
                @Index(name = "idx_documents_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_document_checksum", columnNames = "file_checksum"),
                @UniqueConstraint(name = "uk_document_object_key", columnNames = "object_key")
        }
)
public class Document extends AuditableAbstractAggregateRoot<Document> {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DocumentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DocumentFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DocumentStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "originalFileName", column = @Column(name = "original_file_name", nullable = false)),
            @AttributeOverride(name = "objectKey", column = @Column(name = "object_key", nullable = false)),
            @AttributeOverride(name = "fileChecksum", column = @Column(name = "file_checksum", nullable = false))
    })
    private FileStorageInfo fileStorageInfo;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentChunk> chunks = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentTarget> targets = new HashSet<>();

    public Document() {}

    public Document(String title, Long authorId, Long topicId,
                    DocumentType type, DocumentFormat format, String originalFileName,
                    String objectKey, String fileChecksum) {
        this.title = title;
        this.authorId = authorId;
        this.topicId = topicId;
        this.type = type;
        this.format = format;
        this.status = DocumentStatus.UPLOADED;
        this.fileStorageInfo = new FileStorageInfo(
                originalFileName,
                objectKey,
                fileChecksum
        );
    }

    public void replaceChunks (List<ChunkEmbeddingData> chunkData) {
        if (chunkData == null || chunkData.isEmpty()) {
            throw new DocumentChunksRequiredException(this.getId());
        }

        this.chunks.clear();

        for (var c : chunkData) {
            var chunk = new DocumentChunk(
                    this,
                    c.content(),
                    c.pageNumber(),
                    c.chunkIndex(),
                    c.embedding()
            );
            this.chunks.add(chunk);
        }
    }

    public void addTargets(EducationLevel level, List<GradeLevel> grades, Long courseId) {
        for (GradeLevel grade : grades) {
            targets.add(new DocumentTarget(this, level, grade, courseId));
        }
    }

    public void markAsProcessing() {
        if (this.status != DocumentStatus.UPLOADED) {
            throw new InvalidDocumentStatusTransitionException(this.status, DocumentStatus.PROCESSING);
        }
        this.status = DocumentStatus.PROCESSING;
    }

    public void markAsReady() {
        if (this.status != DocumentStatus.PROCESSING) {
            throw new InvalidDocumentStatusTransitionException(this.status, DocumentStatus.READY);
        }
        if (this.chunks == null || this.chunks.isEmpty()) {
            throw new DocumentChunksRequiredException(this.getId());
        }
        this.status = DocumentStatus.READY;
    }

    public void markAsFailed() {
        if (this.status == DocumentStatus.FAILED || this.status == DocumentStatus.READY) {
            return;
        }
        if (this.status != DocumentStatus.PROCESSING && this.status != DocumentStatus.UPLOADED) {
            throw new InvalidDocumentStatusTransitionException(this.status, DocumentStatus.FAILED);
        }
        this.status = DocumentStatus.FAILED;
    }

    public void confirmUpload() {
        this.registerEvent(new DocumentUploadedEvent(this, this.getId(), this.getFileStorageInfo().getObjectKey()));
    }
}
