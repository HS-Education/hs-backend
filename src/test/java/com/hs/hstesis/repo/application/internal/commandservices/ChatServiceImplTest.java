package com.hs.hstesis.repo.application.internal.commandservices;

import com.hs.hstesis.repo.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.repo.domain.model.aggregates.ChatSession;
import com.hs.hstesis.repo.domain.model.entities.ChatMessage;
import com.hs.hstesis.repo.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.GradeLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.UserEnrollmentContext;
import com.hs.hstesis.repo.infrastructure.outboundservices.ai.AiServiceClient;
import com.hs.hstesis.repo.infrastructure.outboundservices.ai.EmbedQueryResponse;
import com.hs.hstesis.repo.infrastructure.outboundservices.ai.GenerateRequest;
import com.hs.hstesis.repo.infrastructure.outboundservices.ai.GenerateResponse;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.ChatMessageRepository;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.ChatSessionRepository;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentChunkRepository;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentChunkWithMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceImplTest {
    private final ChatSessionRepository sessions = mock(ChatSessionRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final DocumentChunkRepository chunks = mock(DocumentChunkRepository.class);
    private final AiServiceClient ai = mock(AiServiceClient.class);
    private final ExternalLearningService learning = mock(ExternalLearningService.class);
    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(sessions, messages, chunks, ai, learning);
    }

    @Test
    void cannotCreateSessionForCourseOutsideEnrollment() {
        when(learning.getUserEnrollmentContextByCourse(7L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSession(99L, 7L))
                .isInstanceOf(AuthorizationDeniedException.class);
        verifyNoInteractions(sessions, messages, chunks, ai);
    }

    @Test
    void rejectsExistingSessionWhenCourseAccessWasRevokedBeforeCallingAi() {
        when(sessions.findById(1L)).thenReturn(Optional.of(new ChatSession(99L, 7L)));
        when(learning.getUserEnrollmentContextByCourse(7L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMessage(1L, 7L, "question"))
                .isInstanceOf(AuthorizationDeniedException.class);
        verifyNoInteractions(messages, chunks, ai);
    }

    @Test
    void rejectsUnauthorizedStreamBeforeOpeningAiConnection() {
        when(sessions.findById(1L)).thenReturn(Optional.of(new ChatSession(99L, 7L)));
        when(learning.getUserEnrollmentContextByCourse(7L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.streamMessageResponse(1L, 7L, "question",
                ignored -> {}, () -> {}, ignored -> {}))
                .isInstanceOf(AuthorizationDeniedException.class);
        verifyNoInteractions(messages, chunks, ai);
    }

    @Test
    void sendsOnlyAccessibleTargetToAiForEnrolledStudent() {
        when(sessions.findById(1L)).thenReturn(Optional.of(new ChatSession(10L, 7L)));
        when(learning.getUserEnrollmentContextByCourse(7L, 10L)).thenReturn(Optional.of(
                new UserEnrollmentContext(EducationLevel.SECONDARY, GradeLevel.SECOND, 10L)));
        when(ai.embedQuery("question")).thenReturn(new EmbedQueryResponse("fake", 2, List.of(1f, 0f)));
        when(messages.findAllBySessionIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(messages.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DocumentChunkWithMetadata allowed = mock(DocumentChunkWithMetadata.class);
        when(allowed.getTitle()).thenReturn("Allowed");
        when(allowed.getContent()).thenReturn("public lesson");
        when(allowed.getCourseId()).thenReturn(10L);
        when(allowed.getDocumentId()).thenReturn(5L);
        when(allowed.getSimilarity()).thenReturn(0.9);
        when(chunks.findSimilarChunksByAccessibleTarget(eq(10L), eq("SECONDARY"), eq("SECOND"), anyString(), eq(10)))
                .thenReturn(List.of(allowed));
        when(ai.generateAnswer(any(GenerateRequest.class))).thenAnswer(invocation -> {
            GenerateRequest request = invocation.getArgument(0);
            return new GenerateResponse("fake", String.join(" ", request.contextChunks()));
        });

        var response = service.sendMessage(1L, 7L, "question");

        assertThat(response.getContent()).contains("public lesson").doesNotContain("TEACHER_ONLY_CANARY");
        verify(chunks, never()).findSimilarChunksByCourseIdsIn(anyList(), anyString(), anyInt());
    }

    @Test
    void streamFailureDoesNotPersistPartialModelAnswer() {
        when(sessions.findById(1L)).thenReturn(Optional.of(new ChatSession(10L, 7L)));
        when(learning.getUserEnrollmentContextByCourse(7L, 10L)).thenReturn(Optional.of(
                new UserEnrollmentContext(EducationLevel.SECONDARY, GradeLevel.SECOND, 10L)));
        when(ai.embedQuery("question")).thenReturn(new EmbedQueryResponse("fake", 2, List.of(1f, 0f)));
        when(messages.findAllBySessionIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(messages.saveAndFlush(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chunks.findSimilarChunksByAccessibleTarget(eq(10L), eq("SECONDARY"), eq("SECOND"), anyString(), eq(10)))
                .thenReturn(List.of());
        doAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(1);
            Consumer<Throwable> onError = invocation.getArgument(3);
            onToken.accept("partial attacker-influenced answer");
            onError.accept(new java.io.IOException("truncated stream"));
            return null;
        }).when(ai).generateAnswerStream(any(GenerateRequest.class), any(), any(), any());
        var error = new AtomicReference<Throwable>();

        service.streamMessageResponse(1L, 7L, "question", ignored -> {}, () -> {}, error::set);

        var savedMessages = org.mockito.ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages, times(3)).saveAndFlush(savedMessages.capture());
        assertThat(savedMessages.getAllValues().get(2).getContent())
                .isEqualTo("No se pudo completar la respuesta. Intenta enviar el mensaje nuevamente.")
                .doesNotContain("partial attacker-influenced answer");
        var request = org.mockito.ArgumentCaptor.forClass(GenerateRequest.class);
        verify(ai).generateAnswerStream(request.capture(), any(), any(), any());
        assertThat(request.getValue().maxTokens()).isEqualTo(1024);
        assertThat(error.get()).isInstanceOf(java.io.IOException.class);
    }
}
