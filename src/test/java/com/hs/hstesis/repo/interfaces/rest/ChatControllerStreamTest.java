package com.hs.hstesis.repo.interfaces.rest;

import com.hs.hstesis.assessments.interfaces.acl.AssessmentsContextFacade;
import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import com.hs.hstesis.repo.domain.services.ChatService;
import com.hs.hstesis.repo.interfaces.rest.resources.ChatRequestResource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatControllerStreamTest {
    private final ChatService chatService = mock(ChatService.class);
    private final IamContextFacade iamContextFacade = mock(IamContextFacade.class);
    private final AssessmentsContextFacade assessmentsContextFacade = mock(AssessmentsContextFacade.class);
    private final ChatController controller = new ChatController(
            chatService, iamContextFacade, assessmentsContextFacade);

    @Test
    void sendsTokenAndDoneEventsForTheNewStreamingClient() throws Exception {
        when(iamContextFacade.getAuthenticatedUserId()).thenReturn(7L);
        when(assessmentsContextFacade.hasActiveQuiz(7L)).thenReturn(false);
        doAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(3);
            Runnable onComplete = invocation.getArgument(4);
            onToken.accept("respuesta\ncon líneas");
            onComplete.run();
            return null;
        }).when(chatService).streamMessageResponse(eq(4L), eq(7L), eq("pregunta"), any(), any(), any());
        var request = eventStreamRequest();
        var response = new MockHttpServletResponse();

        controller.sendMessageStream(4L, new ChatRequestResource("pregunta"), request, response);

        assertThat(response.getContentType()).startsWith("text/event-stream");
        assertThat(response.getContentAsString())
                .contains("event: token\ndata: {\"text\":\"respuesta\\ncon líneas\"}\n\n")
                .endsWith("event: done\ndata: {}\n\n");
    }

    @Test
    void reportsProviderFailureAfterPartialTokensAsAnErrorEvent() throws Exception {
        when(iamContextFacade.getAuthenticatedUserId()).thenReturn(7L);
        when(assessmentsContextFacade.hasActiveQuiz(7L)).thenReturn(false);
        doAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(3);
            Consumer<Throwable> onError = invocation.getArgument(5);
            onToken.accept("parcial");
            onError.accept(new IOException("sensitive upstream details"));
            return null;
        }).when(chatService).streamMessageResponse(eq(4L), eq(7L), eq("pregunta"), any(), any(), any());
        var response = new MockHttpServletResponse();

        controller.sendMessageStream(4L, new ChatRequestResource("pregunta"), eventStreamRequest(), response);

        assertThat(response.getContentAsString())
                .contains("event: token\ndata: {\"text\":\"parcial\"}\n\n")
                .endsWith("event: error\ndata: {\"code\":\"generation_failed\"}\n\n")
                .doesNotContain("sensitive upstream details");
    }

    @Test
    void keepsRawTextForTheExistingLegacyClient() throws Exception {
        when(iamContextFacade.getAuthenticatedUserId()).thenReturn(7L);
        when(assessmentsContextFacade.hasActiveQuiz(7L)).thenReturn(false);
        doAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(3);
            Runnable onComplete = invocation.getArgument(4);
            onToken.accept("legacy response");
            onComplete.run();
            return null;
        }).when(chatService).streamMessageResponse(eq(4L), eq(7L), eq("pregunta"), any(), any(), any());
        var request = new MockHttpServletRequest();
        request.addHeader("Accept", "*/*");
        var response = new MockHttpServletResponse();

        controller.sendMessageStream(4L, new ChatRequestResource("pregunta"), request, response);

        assertThat(response.getContentType()).startsWith("text/plain");
        assertThat(response.getContentAsString()).isEqualTo("legacy response");
    }

    private static MockHttpServletRequest eventStreamRequest() {
        var request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/event-stream");
        return request;
    }
}
