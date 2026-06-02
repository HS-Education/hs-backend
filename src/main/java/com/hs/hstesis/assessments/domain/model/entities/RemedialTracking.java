package com.hs.hstesis.assessments.domain.model.entities;

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

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public RemedialTracking(Long studentId, Long courseId, Integer weekNumber, Long weakTopicId) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.weekNumber = weekNumber;
        this.weakTopicId = weakTopicId;
        this.isResolved = false;
    }

    public void markAsResolved() {
        this.isResolved = true;
    }
}
