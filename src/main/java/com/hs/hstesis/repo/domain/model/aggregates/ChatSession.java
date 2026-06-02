package com.hs.hstesis.repo.domain.model.aggregates;

import com.hs.hstesis.repo.domain.model.entities.ChatMessage;
import com.hs.hstesis.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "chat_sessions")
public class ChatSession extends AuditableAbstractAggregateRoot<ChatSession> {

    @Column(name = "course_id", nullable = true)
    private Long courseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    protected ChatSession() {}

    public ChatSession(Long courseId, Long userId) {
        this.courseId = courseId;
        this.userId = userId;
    }

    public ChatSession(Long userId) {
        this.userId = userId;
    }

    public void addMessage(String role, String content) {
        this.messages.add(new ChatMessage(this, role, content));
    }

    public void updateCourseId(Long courseId) {
        this.courseId = courseId;
    }
}
