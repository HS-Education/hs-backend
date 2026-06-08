package com.hs.hstesis.repo.interfaces.rest;

import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import com.hs.hstesis.repo.domain.services.ChatService;
import com.hs.hstesis.repo.interfaces.rest.resources.ChatMessageResource;
import com.hs.hstesis.repo.interfaces.rest.resources.ChatRequestResource;
import com.hs.hstesis.repo.interfaces.rest.resources.ChatSessionResource;
import com.hs.hstesis.repo.interfaces.rest.resources.CreateChatSessionResource;
import com.hs.hstesis.repo.interfaces.rest.resources.UpdateChatSessionResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/chat/sessions")
@Tag(name = "Chat", description = "Unified endpoint for both course-specific and global RAG chat sessions")
public class ChatController {

    private final ChatService chatService;
    private final IamContextFacade iamContextFacade;

    public ChatController(ChatService chatService, IamContextFacade iamContextFacade) {
        this.chatService = chatService;
        this.iamContextFacade = iamContextFacade;
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
        
        var message = chatService.sendMessage(sessionId, userId, request.question());
        var resource = new ChatMessageResource(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
        
        return ResponseEntity.ok(resource);
    }
}
