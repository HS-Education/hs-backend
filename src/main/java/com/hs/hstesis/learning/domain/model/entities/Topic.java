package com.hs.hstesis.learning.domain.model.entities;

import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "topics",
        indexes = {
                @Index(name = "idx_topics_course_order", columnList = "course_id, grading_period_id, order_index")
        })
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "order_index")
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_period_id", nullable = false)
    private GradingPeriod gradingPeriod;

    protected Topic() {}

    public Topic(Course course, GradingPeriod gradingPeriod, String name, Integer orderIndex) {
        this.course = course;
        this.gradingPeriod = gradingPeriod;
        this.name = name;
        this.orderIndex = orderIndex;
    }

    public void updateOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public void updateGradingPeriod(GradingPeriod gradingPeriod) {
        this.gradingPeriod = gradingPeriod;
    }
}
