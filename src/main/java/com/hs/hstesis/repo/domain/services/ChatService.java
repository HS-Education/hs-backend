package com.hs.hstesis.repo.domain.services;

import com.hs.hstesis.repo.domain.model.aggregates.ChatSession;
import com.hs.hstesis.repo.domain.model.entities.ChatMessage;

import java.util.List;

public interface ChatService {
    ChatSession createSession(Long courseId, Long userId);
    List<ChatSession> getUserSessions(Long courseId, Long userId);
    List<ChatMessage> getSessionHistory(Long sessionId, Long userId);
    ChatMessage sendMessage(Long sessionId, Long userId, String question);
    void streamMessageResponse(Long sessionId, Long userId, String question,
                               java.util.function.Consumer<String> onToken, Runnable onComplete, java.util.function.Consumer<Throwable> onError);
    ChatSession updateSessionCourse(Long sessionId, Long userId, Long courseId);
    void deleteSession(Long sessionId, Long userId);
    List<ChatSession> getAllUserSessions(Long userId);
}
