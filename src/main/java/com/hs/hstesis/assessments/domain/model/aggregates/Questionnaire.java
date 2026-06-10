package com.hs.hstesis.assessments.domain.model.aggregates;

import com.hs.hstesis.assessments.domain.model.valueobjects.QuestionnaireStatus;
import com.hs.hstesis.assessments.domain.model.valueobjects.QuestionnaireType;
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

    @Column(nullable = false)
    private Integer allowedAttempts;

    @Column(nullable = false)
    private Integer questionsPerAttempt;

    @Column(nullable = true)
    private Long targetStudentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'NORMAL'")
    private QuestionnaireType type;

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public Integer getAllowedAttempts() {
        return allowedAttempts;
    }

    public Integer getQuestionsPerAttempt() {
        return questionsPerAttempt;
    }

    public QuestionnaireStatus getStatus() {
        return status;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionnaireStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Questionnaire(Long courseId, Long gradingPeriodId, Integer weekNumber, Integer allowedAttempts, Integer questionsPerAttempt) {
        this.courseId = courseId;
        this.gradingPeriodId = gradingPeriodId;
        this.weekNumber = weekNumber;
        this.allowedAttempts = allowedAttempts;
        this.questionsPerAttempt = questionsPerAttempt;
        this.status = QuestionnaireStatus.DRAFT;
        this.type = QuestionnaireType.NORMAL;
    }

    public Questionnaire(Long courseId, Long gradingPeriodId, Integer weekNumber, Integer allowedAttempts, Integer questionsPerAttempt, Long targetStudentId, QuestionnaireType type) {
        this.courseId = courseId;
        this.gradingPeriodId = gradingPeriodId;
        this.weekNumber = weekNumber;
        this.allowedAttempts = allowedAttempts;
        this.questionsPerAttempt = questionsPerAttempt;
        this.status = QuestionnaireStatus.DRAFT;
        this.targetStudentId = targetStudentId;
        this.type = type;
    }
}
