package com.hs.hstesis.learning.domain.model.entities;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "area_coordinators", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "area_id"})
})
@Getter
@Setter
public class AreaCoordinator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    protected AreaCoordinator() {}

    public AreaCoordinator(User user, Area area) {
        this.user = user;
        this.area = area;
    }
}
