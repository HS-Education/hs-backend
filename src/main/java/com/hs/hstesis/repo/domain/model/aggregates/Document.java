package com.hs.hstesis.repo.domain.model.aggregates;

import com.hs.hstesis.repo.domain.model.commands.UploadDocumentCommand;
import com.hs.hstesis.repo.domain.model.entities.DocumentChunk;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentFormat;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentStatus;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentType;
import com.hs.hstesis.repo.domain.model.valueobjects.FileStorageInfo;
import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "documents",
        indexes = {
                @Index(name = "idx_documents_topic", columnList = "topic_id"),
                @Index(name = "idx_documents_user", columnList = "uploaded_by"),
                @Index(name = "idx_documents_status", columnList = "status")
        })
public class Document extends AuditableAbstractAggregateRoot<Document> {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedByUserId;

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

    public Document() {}

    public Document(UploadDocumentCommand command) {
        this.title = command.title();
        this.uploadedByUserId = command.uploadedByUserId();
        this.topicId = command.topicId();
        this.type = command.type();
        this.format = command.format();
        this.status = DocumentStatus.UPLOADED;

        this.fileStorageInfo = new FileStorageInfo(
                command.originalFileName(),
                command.objectKey(),
                command.fileChecksum()
        );
    }

    public void addChunk(String content, int pageNumber, int chunkIndex, float[] embedding) {
        DocumentChunk chunk = new DocumentChunk(this, content, pageNumber, chunkIndex, embedding);
        chunks.add(chunk);
    }

    public void removeChunk(DocumentChunk chunk) {
        chunks.remove(chunk);
    }
}
