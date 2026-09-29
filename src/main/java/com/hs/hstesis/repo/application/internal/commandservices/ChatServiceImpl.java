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
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentChunkWithMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServiceImpl.class);


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
            requireCourseAccess(userId, courseId);
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

        requireCourseAccess(userId, session.getCourseId());

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

        requireCourseAccess(userId, session.getCourseId());
        var userMsg = new ChatMessage(session, "user", question);
        chatMessageRepository.saveAndFlush(userMsg);

        long preparationStartedAt = System.nanoTime();
        var embedResponse = aiServiceClient.embedQuery(question);
        long embeddingMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - preparationStartedAt);
        var vectorString = formatVectorForPg(embedResponse.embedding());
        long retrievalStartedAt = System.nanoTime();
        List<String> contextTexts = accessibleContext(userId, session.getCourseId(), vectorString);
        long retrievalMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - retrievalStartedAt);
        LOGGER.info("Sery chat context prepared (sessionId={}, sourceChunks={}, embeddingMs={}, retrievalMs={})",
                sessionId, contextTexts.size(), embeddingMs, retrievalMs);

        var history = chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
        var messagesForAi = history.stream()
                .map(msg -> new AiMessageDto(msg.getRole(), msg.getContent()))
                .toList();

        var req = new GenerateRequest(messagesForAi, contextTexts, 1024, 0.2f, false);

        var generateResponse = aiServiceClient.generateAnswer(req);

        var msg = new ChatMessage(session, "assistant", generateResponse.answer());
        return chatMessageRepository.save(msg);
    }

    @Override
    public void streamMessageResponse(Long sessionId, Long userId, String question,
            java.util.function.Consumer<String> onToken, Runnable onComplete,
            java.util.function.Consumer<Throwable> onError) {
        var session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("Not your session");
        }

        requireCourseAccess(userId, session.getCourseId());
        var userMsg = new ChatMessage(session, "user", question);
        chatMessageRepository.saveAndFlush(userMsg);

        long preparationStartedAt = System.nanoTime();
        var embedResponse = aiServiceClient.embedQuery(question);
        long embeddingMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - preparationStartedAt);
        var vectorString = formatVectorForPg(embedResponse.embedding());
        long retrievalStartedAt = System.nanoTime();
        List<String> contextTexts = accessibleContext(userId, session.getCourseId(), vectorString);
        long retrievalMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - retrievalStartedAt);
        LOGGER.info("Sery chat context prepared (sessionId={}, sourceChunks={}, embeddingMs={}, retrievalMs={})",
                sessionId, contextTexts.size(), embeddingMs, retrievalMs);

        var history = chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
        var messagesForAi = history.stream()
                .map(msg -> new AiMessageDto(msg.getRole(), msg.getContent()))
                .toList();

        var req = new GenerateRequest(messagesForAi, contextTexts, 1024, 0.2f, true);
        StringBuilder fullAnswer = new StringBuilder();
        var assistantMsg = chatMessageRepository.saveAndFlush(new ChatMessage(session, "assistant", ""));
        var responsePersisted = new java.util.concurrent.atomic.AtomicBoolean(false);

        Runnable persistResponse = () -> {
            if (!responsePersisted.compareAndSet(false, true)) return;
            String answer = fullAnswer.toString();
            if (answer.isBlank()) {
                answer = "No se pudo completar la respuesta. Intenta enviar el mensaje nuevamente.";
            }
            assistantMsg.updateContent(answer);
            chatMessageRepository.saveAndFlush(assistantMsg);
        };

        aiServiceClient.generateAnswerStream(req,
                token -> {
                    fullAnswer.append(token);
                    onToken.accept(token);
                },
                () -> {
                    try {
                        persistResponse.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    onComplete.run();
                },
                error -> {
                    try {
                        // Never persist a partial model answer as if generation succeeded.
                        fullAnswer.setLength(0);
                        persistResponse.run();
                    } catch (Exception persistenceError) {
                        persistenceError.printStackTrace();
                    }
                    onError.accept(error);
                });
    }

    @Transactional
    public void saveAssistantMessage(ChatSession session, String text) {
        var msg = new ChatMessage(session, "assistant", text);
        chatMessageRepository.save(msg);
    }

    private String formatVectorForPg(List<Float> embedding) {
        return "[" + embedding.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }

    private void requireCourseAccess(Long userId, Long courseId) {
        if (courseId == null || courseId == 0L) return;
        if (externalLearningService.doesCoordinatorOwnCourse(userId, courseId)) return;
        if (externalLearningService.getUserEnrollmentContextByCourse(userId, courseId).isPresent()) return;
        throw new AuthorizationDeniedException("Course is not accessible");
    }

    private List<String> accessibleContext(Long userId, Long selectedCourseId, String vector) {
        List<Long> courseIds = selectedCourseId != null && selectedCourseId != 0L
                ? List.of(selectedCourseId)
                : externalLearningService.getEnrolledCourseIds(userId);
        return courseIds.stream()
                .distinct()
                .flatMap(courseId -> {
                    List<DocumentChunkWithMetadata> chunks;
                    if (externalLearningService.doesCoordinatorOwnCourse(userId, courseId)) {
                        chunks = documentChunkRepository.findSimilarChunksByCourseIdsIn(List.of(courseId), vector, 10);
                    } else {
                        var enrollment = externalLearningService.getUserEnrollmentContextByCourse(userId, courseId);
                        if (enrollment.isEmpty()) return java.util.stream.Stream.empty();
                        var context = enrollment.get();
                        chunks = documentChunkRepository.findSimilarChunksByAccessibleTarget(
                                courseId, context.educationLevel().name(), context.gradeLevel().name(), vector, 10);
                    }
                    return chunks.stream();
                })
                .sorted(Comparator.comparing(DocumentChunkWithMetadata::getSimilarity,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(c -> String.format(
                        "Fuente: %s\nEnlace de descarga: /api/v1/courses/%d/documents/%d/download\nContenido: %s",
                        c.getTitle(), c.getCourseId(), c.getDocumentId(), c.getContent()))
                .toList();
    }

    @Override
    @Transactional
    public ChatSession updateSessionCourse(Long sessionId, Long userId, Long courseId) {
        var session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("Not your session");
        }

        requireCourseAccess(userId, session.getCourseId());
        Long validCourseId = (courseId != null && courseId != 0L) ? courseId : null;
        requireCourseAccess(userId, validCourseId);
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
