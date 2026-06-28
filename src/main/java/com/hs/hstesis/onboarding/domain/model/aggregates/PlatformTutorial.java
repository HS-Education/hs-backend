package com.hs.hstesis.onboarding.domain.model.aggregates;

import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "platform_tutorials")
@Getter
@Setter
public class PlatformTutorial extends AuditableAbstractAggregateRoot<PlatformTutorial> {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    public PlatformTutorial() {
    }

    public PlatformTutorial(String title, String description, String fileUrl) {
        this.title = title;
        this.description = description;
        this.fileUrl = fileUrl;
    }
}

