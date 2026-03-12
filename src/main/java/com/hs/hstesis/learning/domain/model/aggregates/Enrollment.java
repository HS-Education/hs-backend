package com.hs.hstesis.learning.domain.model.aggregates;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "classroom_id"})
})
@Getter
@Setter
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "role_in_classroom", nullable = false)
    private String roleInClassroom;

    protected Enrollment() {}

    public Enrollment(Long userId, Classroom classroom, String roleInClassroom) {
        this.userId = userId;
        this.classroom = classroom;
        this.roleInClassroom = roleInClassroom;
    }
}
