package com.hs.hstesis.repo.domain.model.entities;

import com.hs.hstesis.repo.domain.model.aggregates.Document;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(
        name = "document_chunks",
        indexes = {
                @Index(name = "idx_chunks_document", columnList = "document_id"),
                @Index(name = "idx_chunks_doc_chunk", columnList = "document_id, chunk_index")
        }
)
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(768)")
    private float[] embedding;

    protected DocumentChunk() {}

    public DocumentChunk(Document document,
                         String content,
                         Integer pageNumber,
                         Integer chunkIndex,
                         float[] embedding) {
        this.document = document;
        this.content = content;
        this.pageNumber = pageNumber;
        this.chunkIndex = chunkIndex;
        this.embedding = embedding;
    }
}
