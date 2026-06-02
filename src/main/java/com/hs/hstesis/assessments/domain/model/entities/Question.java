package com.hs.hstesis.assessments.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_instance_id", nullable = false)
    private QuestionnaireInstance questionnaireInstance;

    @Column(nullable = false)
    private Long topicId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Convert(converter = com.hs.hstesis.shared.infrastructure.persistance.jpa.StringListConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private List<String> options;

    @Column(nullable = false)
    private Integer correctOptionIndex;

    @Column(nullable = false)
    private Boolean isRemedial;

    public Question(QuestionnaireInstance questionnaireInstance, Long topicId, String text, List<String> options, Integer correctOptionIndex, Boolean isRemedial) {
        this.questionnaireInstance = questionnaireInstance;
        this.topicId = topicId;
        this.text = text;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.isRemedial = isRemedial;
    }
}
