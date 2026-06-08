package com.hs.hstesis.repo.application.internal.commandservices;

import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.repo.domain.model.aggregates.ChatSession;
import com.hs.hstesis.repo.domain.model.entities.ChatMessage;
import com.hs.hstesis.repo.domain.services.ChatService;
import com.hs.hstesis.repo.infrastructure.outboundservices.ai.AiMessageDto;
import com.hs.hstesis.repo.infrastructure.outboundservices.ai.AiServiceClient;
import com.hs.hstesis.repo.infrastructure.outboundservices.ai.GenerateRequest;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.ChatMessageRepository;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.ChatSessionRepository;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentChunkRepository;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final AiServiceClient aiServiceClient;
    private final ExternalLearningService externalLearningService;

    public ChatServiceImpl(ChatSessionRepository chatSessionRepository,
                           ChatMessageRepository chatMessageRepository,
                           DocumentChunkRepository documentChunkRepository,
                           AiServiceClient aiServiceClient,
                           ExternalLearningService externalLearningService) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.aiServiceClient = aiServiceClient;
        this.externalLearningService = externalLearningService;
    }

    @Override
    @Transactional
    public ChatSession createSession(Long courseId, Long userId) {
        if (courseId != null) {
            return chatSessionRepository.save(new ChatSession(courseId, userId));
        } else {
            return chatSessionRepository.save(new ChatSession(userId));
        }
    }

    @Override
    public List<ChatSession> getUserSessions(Long courseId, Long userId) {
        if (courseId != null) {
            return chatSessionRepository.findAllByCourseIdAndUserId(courseId, userId);
        } else {
            return chatSessionRepository.findAllByCourseIdAndUserId(null, userId);
        }
    }

    @Override
    public List<ChatMessage> getSessionHistory(Long sessionId, Long userId) {
        var session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("Not your session");
        }

        return chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Override
    @Transactional
    public ChatMessage sendMessage(Long sessionId, Long userId, String question) {
        var session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("Not your session");
        }

        var userMsg = new ChatMessage(session, "user", question);
        chatMessageRepository.save(userMsg);

        List<Long> courseIdsToSearch;
        if (session.getCourseId() != null && session.getCourseId() != 0L) {
            courseIdsToSearch = List.of(session.getCourseId());
            System.out.println("Using manual courseId: " + courseIdsToSearch);
        } else {
            courseIdsToSearch = new java.util.ArrayList<>(externalLearningService.getEnrolledCourseIds(userId));
            System.out.println("Using enrolled courses for userId " + userId + ": " + courseIdsToSearch);
        }

        var embedResponse = aiServiceClient.embedQuery(question);
        var vectorString = formatVectorForPg(embedResponse.embedding());

        List<String> contextTexts = List.of();
        if (!courseIdsToSearch.isEmpty()) {
            var chunksWithMeta = documentChunkRepository.findSimilarChunksByCourseIdsIn(courseIdsToSearch, vectorString, 10);
            contextTexts = chunksWithMeta.stream()
                .map(c -> String.format("Fuente: %s\nEnlace de descarga: /api/v1/courses/%d/documents/%d/download\nContenido: %s", 
                     c.getTitle(), c.getCourseId(), c.getDocumentId(), c.getContent()))
                .toList();
        }

        var history = chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
        var messagesForAi = history.stream()
                .map(msg -> new AiMessageDto(msg.getRole(), msg.getContent()))
                .toList();

        var req = new GenerateRequest(messagesForAi, contextTexts, 512, 0.2f, false);

        var generateResponse = aiServiceClient.generateAnswer(req);

        var msg = new ChatMessage(session, "assistant", generateResponse.answer());
        return chatMessageRepository.save(msg);
    }

    @Transactional
    public void saveAssistantMessage(ChatSession session, String text) {
        var msg = new ChatMessage(session, "assistant", text);
        chatMessageRepository.save(msg);
    }

    private String formatVectorForPg(List<Float> embedding) {
        return "[" + embedding.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }

    @Override
    @Transactional
    public ChatSession updateSessionCourse(Long sessionId, Long userId, Long courseId) {
        var session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("Not your session");
        }

        Long validCourseId = (courseId != null && courseId != 0L) ? courseId : null;
        session.updateCourseId(validCourseId);
        
        return chatSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        var session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("Not your session");
        }

        chatSessionRepository.delete(session);
    }

    @Override
    public List<ChatSession> getAllUserSessions(Long userId) {
        return chatSessionRepository.findAllByUserIdOrderByIdAsc(userId);
    }
}
