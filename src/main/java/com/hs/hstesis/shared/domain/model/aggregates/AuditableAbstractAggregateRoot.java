package com.hs.hstesis.shared.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.domain.AbstractAggregateRoot;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Getter
@MappedSuperclass
public abstract class AuditableAbstractAggregateRoot<T extends AbstractAggregateRoot<T>> extends AbstractAggregateRoot<T> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(nullable = false, updatable = false)
    private Date createdAt;

    @Column(nullable = false)
    private Date updatedAt;

    @PrePersist
    public void prePersist() {
        ZonedDateTime nowInLima = ZonedDateTime.now(ZoneId.of("America/Lima"));
        this.createdAt = Date.from(nowInLima.toInstant());
        this.updatedAt = Date.from(nowInLima.toInstant());
    }

    @PreUpdate
    public void preUpdate() {
        ZonedDateTime nowInLima = ZonedDateTime.now(ZoneId.of("America/Lima"));
        this.updatedAt = Date.from(nowInLima.toInstant());
    }
}