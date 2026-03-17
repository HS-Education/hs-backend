package com.hs.hstesis.repo.domain.model.entities;

import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentTargetId;
import com.hs.hstesis.repo.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.GradeLevel;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "document_targets",
        indexes = {
                @Index(
                        name = "idx_targets_lookup",
                        columnList = "education_level, grade_level, course_id"
                )
        }
)
public class DocumentTarget {

    @EmbeddedId
    private DocumentTargetId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("documentId")
    @JoinColumn(name = "document_id")
    private Document document;

    protected DocumentTarget() {}

    public DocumentTarget(Document document,
                          EducationLevel educationLevel,
                          GradeLevel gradeLevel,
                          Long courseId) {
        this.document = document;

        this.id = new DocumentTargetId(document.getId(), educationLevel, gradeLevel, courseId);
    }
}
