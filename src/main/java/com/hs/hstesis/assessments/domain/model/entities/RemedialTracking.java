package com.hs.hstesis.assessments.domain.model.entities;

import com.hs.hstesis.assessments.domain.model.valueobjects.QuestionDifficulty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "remedial_trackings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class RemedialTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Integer weekNumber;

    @Column(nullable = false)
    private Long weakTopicId;

    @Column(nullable = false)
    private Boolean isResolved;

    private Integer requiredQuestionCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QuestionDifficulty difficulty;

    private Integer lastRemedialScore;

    private Boolean consolidationMode;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public RemedialTracking(Long studentId, Long courseId, Integer weekNumber, Long weakTopicId,
                            Integer requiredQuestionCount, QuestionDifficulty difficulty) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.weekNumber = weekNumber;
        this.weakTopicId = weakTopicId;
        this.isResolved = false;
        this.requiredQuestionCount = requiredQuestionCount;
        this.difficulty = difficulty;
        this.consolidationMode = false;
    }

    public void markAsResolved() {
        this.isResolved = true;
    }

    public int questionsToAssign() {
        return requiredQuestionCount != null ? requiredQuestionCount : 2;
    }

    public QuestionDifficulty effectiveDifficulty() {
        return difficulty != null ? difficulty : QuestionDifficulty.LOW;
    }

    public boolean isInConsolidationMode() {
        return Boolean.TRUE.equals(consolidationMode);
    }

    public void updateRemediation(int score, int questionCount, QuestionDifficulty newDifficulty, boolean consolidation) {
        this.lastRemedialScore = score;
        this.requiredQuestionCount = questionCount;
        this.difficulty = newDifficulty;
        this.consolidationMode = consolidation;
    }
}
