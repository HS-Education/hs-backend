package com.hs.hstesis.repo.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import com.hs.hstesis.repo.domain.services.ChatService;
import com.hs.hstesis.repo.interfaces.rest.resources.ChatMessageResource;
import com.hs.hstesis.repo.interfaces.rest.resources.ChatRequestResource;
import com.hs.hstesis.repo.interfaces.rest.resources.ChatSessionResource;
import com.hs.hstesis.repo.interfaces.rest.resources.CreateChatSessionResource;
import com.hs.hstesis.repo.interfaces.rest.resources.UpdateChatSessionResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping(value = "/api/v1/chat/sessions")
@Tag(name = "Chat", description = "Unified endpoint for both course-specific and global RAG chat sessions")
public class ChatController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    private static final ObjectMapper STREAM_MAPPER = new ObjectMapper();


    private final ChatService chatService;
    private final IamContextFacade iamContextFacade;
    private final com.hs.hstesis.assessments.interfaces.acl.AssessmentsContextFacade assessmentsContextFacade;

    public ChatController(ChatService chatService, IamContextFacade iamContextFacade, com.hs.hstesis.assessments.interfaces.acl.AssessmentsContextFacade assessmentsContextFacade) {
        this.chatService = chatService;
        this.iamContextFacade = iamContextFacade;
        this.assessmentsContextFacade = assessmentsContextFacade;
    }

    @PreAuthorize("hasAuthority('REPOSITORY_READ')")
    @Operation(summary = "Create a new chat session (global or course-specific)")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatSessionResource> createSession(@RequestBody(required = false) CreateChatSessionResource request) {
        var userId = iamContextFacade.getAuthenticatedUserId();
        var courseId = (request != null && request.courseId() != null && request.courseId() != 0L) ? request.courseId() : null;
        
        var session = chatService.createSession(courseId, userId);
        
        var allSessions = chatService.getAllUserSessions(userId);
        int sessionNumber = 1;
        for (int i = 0; i < allSessions.size(); i++) {
            if (allSessions.get(i).getId().equals(session.getId())) {
                sessionNumber = i + 1;
                break;
            }
        }
        
        var resource = new ChatSessionResource(session.getId(), session.getCourseId(), session.getUserId(), sessionNumber);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('REPOSITORY_READ')")
    @Operation(summary = "Get chat sessions (globally or filtered by courseId)")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChatSessionResource>> getSessions(@RequestParam(required = false) Long courseId) {
        var userId = iamContextFacade.getAuthenticatedUserId();
        
        Long validCourseId = (courseId != null && courseId != 0L) ? courseId : null;
        var sessions = chatService.getUserSessions(validCourseId, userId);
        
        var allSessions = chatService.getAllUserSessions(userId);
        java.util.Map<Long, Integer> sessionNumberMap = new java.util.HashMap<>();
        for (int i = 0; i < allSessions.size(); i++) {
            sessionNumberMap.put(allSessions.get(i).getId(), i + 1);
        }
        
        var resources = sessions.stream()
                .map(s -> new ChatSessionResource(s.getId(), s.getCourseId(), s.getUserId(), sessionNumberMap.getOrDefault(s.getId(), 1)))
                .toList();
                
        return ResponseEntity.ok(resources);
    }

    @PreAuthorize("hasAuthority('REPOSITORY_READ')")
    @Operation(summary = "Update the course of an existing chat session")
    @PatchMapping(value = "/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatSessionResource> updateSessionCourse(
            @PathVariable Long sessionId,
            @RequestBody UpdateChatSessionResource request) {
            
        var userId = iamContextFacade.getAuthenticatedUserId();
        
        var session = chatService.updateSessionCourse(sessionId, userId, request.courseId());
        
        var allSessions = chatService.getAllUserSessions(userId);
        int sessionNumber = 1;
        for (int i = 0; i < allSessions.size(); i++) {
            if (allSessions.get(i).getId().equals(session.getId())) {
                sessionNumber = i + 1;
                break;
            }
        }
        
        var resource = new ChatSessionResource(session.getId(), session.getCourseId(), session.getUserId(), sessionNumber);
        return ResponseEntity.ok(resource);
    }

    @PreAuthorize("hasAuthority('REPOSITORY_READ')")
    @Operation(summary = "Delete a chat session")
    @DeleteMapping(value = "/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        var userId = iamContextFacade.getAuthenticatedUserId();
        chatService.deleteSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('REPOSITORY_READ')")
    @Operation(summary = "Get chat history for a specific session")
    @GetMapping(value = "/{sessionId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChatMessageResource>> getSessionHistory(@PathVariable Long sessionId) {
        var userId = iamContextFacade.getAuthenticatedUserId();
        var messages = chatService.getSessionHistory(sessionId, userId);
        
        var resources = messages.stream()
                .map(m -> new ChatMessageResource(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
                
        return ResponseEntity.ok(resources);
    }

    @PreAuthorize("hasAuthority('REPOSITORY_READ')")
    @Operation(summary = "Send a message and receive response")
    @PostMapping(value = "/{sessionId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatMessageResource> sendMessage(
            @PathVariable Long sessionId,
            @RequestBody @Valid ChatRequestResource request) {
            
        var userId = iamContextFacade.getAuthenticatedUserId();
        
        if (assessmentsContextFacade.hasActiveQuiz(userId)) {
            throw new IllegalStateException("Sery está desactivado porque tienes un cuestionario en curso. Debes finalizarlo antes de usar el chat.");
        }
        
        var message = chatService.sendMessage(sessionId, userId, request.question());
        var resource = new ChatMessageResource(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
        
        return ResponseEntity.ok(resource);
    }

    @PreAuthorize("hasAuthority('REPOSITORY_READ')")
    @Operation(summary = "Send a message and receive response as a stream")
    @PostMapping(value = "/{sessionId}/stream")
    public void sendMessageStream(
            @PathVariable Long sessionId,
            @RequestBody @Valid ChatRequestResource request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) throws IOException {
            
        var userId = iamContextFacade.getAuthenticatedUserId();
        
        if (assessmentsContextFacade.hasActiveQuiz(userId)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("Sery está desactivado porque tienes un cuestionario en curso. Debes finalizarlo antes de usar el chat.");
            return;
        }
        
        boolean useEventStream = httpRequest.getHeader("Accept") != null
                && httpRequest.getHeader("Accept").toLowerCase(java.util.Locale.ROOT)
                        .contains(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setContentType(useEventStream
                ? MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8"
                : "text/plain; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setBufferSize(1);
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        
        var outputStream = response.getOutputStream();
        var clientConnected = new java.util.concurrent.atomic.AtomicBoolean(true);
        var firstTokenTimeMs = new AtomicLong(-1);
        long startedAt = System.nanoTime();
        
        try {
            chatService.streamMessageResponse(sessionId, userId, request.question(),
                token -> {
                    firstTokenTimeMs.compareAndSet(-1, elapsedMillis(startedAt));
                    if (useEventStream) {
                        writeEvent(outputStream, clientConnected, "token", Map.of("text", token));
                    } else {
                        writeLegacyChunk(outputStream, clientConnected, token);
                    }
                },
                () -> {
                    if (useEventStream) {
                        writeEvent(outputStream, clientConnected, "done", Map.of());
                    } else if (clientConnected.get()) {
                        try {
                            outputStream.flush();
                        } catch (IOException ignored) {}
                    }
                    LOGGER.info("Sery chat stream completed (sessionId={}, elapsedMs={}, firstTokenMs={})",
                            sessionId, elapsedMillis(startedAt), firstTokenTimeMs.get());
                },
                error -> {
                    LOGGER.warn("Sery chat stream failed (sessionId={}, elapsedMs={}, firstTokenMs={}, cause={})",
                            sessionId, elapsedMillis(startedAt), firstTokenTimeMs.get(), rootCauseType(error));
                    if (useEventStream) {
                        writeEvent(outputStream, clientConnected, "error", Map.of("code", "generation_failed"));
                    } else if (clientConnected.get()) {
                        writeLegacyChunk(outputStream, clientConnected,
                                "\n\n**Error:** La respuesta de Sery se interrumpió y no se completó. Intenta de nuevo.");
                    }
                }
            );
        } catch (RuntimeException error) {
            LOGGER.warn("Sery chat preparation failed (sessionId={}, elapsedMs={}, cause={})",
                    sessionId, elapsedMillis(startedAt), rootCauseType(error));
            throw error;
        }
    }

    private static void writeEvent(OutputStream outputStream, AtomicBoolean clientConnected,
            String event, Map<String, ?> payload) {
        if (!clientConnected.get()) return;
        try {
            String frame = "event: " + event + "\ndata: " + STREAM_MAPPER.writeValueAsString(payload) + "\n\n";
            outputStream.write(frame.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException error) {
            clientConnected.set(false);
        }
    }

    private static void writeLegacyChunk(OutputStream outputStream, AtomicBoolean clientConnected, String token) {
        if (!clientConnected.get()) return;
        try {
            outputStream.write(token.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException error) {
            // Keep generating and persist the complete answer after the client disconnects.
            clientConnected.set(false);
        }
    }

    private static long elapsedMillis(long startedAt) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static String rootCauseType(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        return cause.getClass().getSimpleName();
    }
}
