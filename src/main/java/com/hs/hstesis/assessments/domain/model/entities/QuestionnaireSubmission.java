package com.hs.hstesis.assessments.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "questionnaire_submissions", uniqueConstraints = @UniqueConstraint(
        name = "uk_submission_instance_student", columnNames = {"questionnaire_instance_id", "student_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class QuestionnaireSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_instance_id", nullable = false)
    private QuestionnaireInstance questionnaireInstance;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_status", nullable = false, length = 20,
            columnDefinition = "varchar(20) default 'NOT_REQUIRED'")
    private com.hs.hstesis.shared.domain.model.valueobjects.QuestionnaireFeedbackStatus feedbackStatus =
            com.hs.hstesis.shared.domain.model.valueobjects.QuestionnaireFeedbackStatus.NOT_REQUIRED;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<QuestionnaireSubmissionAnswer> answers = new java.util.ArrayList<>();

    public QuestionnaireSubmission(QuestionnaireInstance questionnaireInstance, Long studentId, Integer score) {
        this.questionnaireInstance = questionnaireInstance;
        this.studentId = studentId;
        this.score = score;
    }

    public QuestionnaireSubmissionAnswer addAnswer(Question question, Integer selectedOptionIndex, Boolean isCorrect) {
        var answer = new QuestionnaireSubmissionAnswer(this, question, selectedOptionIndex, isCorrect);
        this.answers.add(answer);
        return answer;
    }

    public void markFeedbackPending() {
        this.feedbackStatus = com.hs.hstesis.shared.domain.model.valueobjects.QuestionnaireFeedbackStatus.PENDING;
    }

    public void markFeedbackProcessing() {
        this.feedbackStatus = com.hs.hstesis.shared.domain.model.valueobjects.QuestionnaireFeedbackStatus.PROCESSING;
    }

    public void markFeedbackReady() {
        this.feedbackStatus = com.hs.hstesis.shared.domain.model.valueobjects.QuestionnaireFeedbackStatus.READY;
    }

    public void markFeedbackNotRequired() {
        this.feedbackStatus = com.hs.hstesis.shared.domain.model.valueobjects.QuestionnaireFeedbackStatus.NOT_REQUIRED;
    }

    public void markFeedbackFailed() {
        this.feedbackStatus = com.hs.hstesis.shared.domain.model.valueobjects.QuestionnaireFeedbackStatus.FAILED;
    }

    public void clearFeedback() {
        this.answers.forEach(answer -> answer.setAiFeedback(null));
    }
}
