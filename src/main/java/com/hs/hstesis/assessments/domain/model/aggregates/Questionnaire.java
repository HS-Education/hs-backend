package com.hs.hstesis.assessments.domain.model.aggregates;

import com.hs.hstesis.assessments.domain.model.valueobjects.QuestionnaireStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "questionnaires")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Questionnaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long gradingPeriodId;

    @Column(nullable = false)
    private Integer weekNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionnaireStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Questionnaire(Long courseId, Long gradingPeriodId, Integer weekNumber) {
        this.courseId = courseId;
        this.gradingPeriodId = gradingPeriodId;
        this.weekNumber = weekNumber;
        this.status = QuestionnaireStatus.DRAFT;
    }
}
