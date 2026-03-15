//package com.hs.hstesis.repo.domain.model.aggregates;
//
//import com.hs.hstesis.repo.domain.model.valueobjects.DocumentFormat;
//import com.hs.hstesis.repo.domain.model.valueobjects.DocumentStatus;
//import com.hs.hstesis.repo.domain.model.valueobjects.DocumentType;
//import com.hs.hstesis.repo.domain.model.valueobjects.FileStorageInfo;
//import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
//import jakarta.persistence.*;
//
//@Entity
//public class Document extends AuditableAbstractAggregateRoot<Document> {
//    private String title;
//
//    @Embedded
//    private FileStorageInfo fileStorageInfo;
//
//    @Column(nullable = false)
//    private Long uploadedByUserId;
//
//    @Column(nullable = false)
//    private Long topicId;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private DocumentType type;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private DocumentFormat format;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private DocumentStatus status;
//
//    protected Document() {}
//
//}
