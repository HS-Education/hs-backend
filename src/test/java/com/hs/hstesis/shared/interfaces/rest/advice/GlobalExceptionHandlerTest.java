package com.hs.hstesis.shared.interfaces.rest.advice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
    @Test void disconnectedResponseIsNotWrittenToOrReconfigured() {
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        new GlobalExceptionHandler().handleDisconnectedResponse(
                new org.springframework.web.context.request.async.AsyncRequestNotUsableException("Synthetic disconnect"), response);
        verifyNoInteractions(response);
    }

    @Test void asyncTimeoutReturns503WithoutJsonOrChangingCommittedResponses() {
        var handler = new GlobalExceptionHandler();
        var timeout = new org.springframework.web.context.request.async.AsyncRequestTimeoutException();
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        response.setContentType("text/event-stream");
        handler.handleAsyncTimeout(timeout, response);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsByteArray()).isEmpty();
        var committed = mock(jakarta.servlet.http.HttpServletResponse.class);
        when(committed.isCommitted()).thenReturn(true);
        handler.handleAsyncTimeout(timeout, committed);
        verify(committed, never()).setStatus(anyInt());
    }

    @Test void mvcResolvesDisconnectedSseWithoutTryingToConvertJson() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new SyntheticStreamController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/synthetic-disconnect").accept("text/event-stream")).andReturn();
        assertThat(result.getResolvedException()).isInstanceOf(
                org.springframework.web.context.request.async.AsyncRequestNotUsableException.class);
        assertThat(result.getResponse().getContentAsByteArray()).isEmpty();
        var timeout = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/synthetic-timeout").accept("text/event-stream")).andReturn();
        assertThat(timeout.getResponse().getStatus()).isEqualTo(503);
        assertThat(timeout.getResponse().getContentAsByteArray()).isEmpty();
    }

    @org.springframework.web.bind.annotation.RestController
    static class SyntheticStreamController {
        @org.springframework.web.bind.annotation.GetMapping(value = "/synthetic-disconnect", produces = "text/event-stream")
        public void disconnect(jakarta.servlet.http.HttpServletResponse response)
                throws org.springframework.web.context.request.async.AsyncRequestNotUsableException {
            response.setContentType("text/event-stream");
            throw new org.springframework.web.context.request.async.AsyncRequestNotUsableException("Synthetic disconnect");
        }

        @org.springframework.web.bind.annotation.GetMapping(value = "/synthetic-timeout", produces = "text/event-stream")
        public void timeout(jakarta.servlet.http.HttpServletResponse response) {
            response.setContentType("text/event-stream");
            throw new org.springframework.web.context.request.async.AsyncRequestTimeoutException();
        }
    }

    @Test void missingHandlerDoesNotExposeTheRequestOrHeaders() {
        var exception = mock(org.springframework.web.servlet.NoHandlerFoundException.class);
        var response = new GlobalExceptionHandler().handleMissingRoute(exception);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("The requested resource was not found.");
        verifyNoInteractions(exception);
    }

    @Test void frameworkServerErrorsAreNotHiddenAsClientErrors() {
        var exception = new org.springframework.web.ErrorResponseException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        var response = new GlobalExceptionHandler().handleInternalError(exception);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).doesNotContain("PRIVATE_VARIABLE");
    }

    @Test
    void openingPostOnlyEndpointsWithGetReturns405AndAllowInsteadOf500() {
        var exception = new HttpRequestMethodNotSupportedException("GET", java.util.List.of("POST"));
        var response = new GlobalExceptionHandler().handleUnsupportedMethod(exception);
        assertThat(response.getStatusCode().value()).isEqualTo(405);
        assertThat(response.getHeaders().getAllow()).containsExactly(org.springframework.http.HttpMethod.POST);
        assertThat(response.getBody().message()).isEqualTo("This HTTP method is not supported for this endpoint.");
    }

    @Test
    void invalidQueryParameterReturnsBadRequestWithoutEchoingInput() {
        var exception = mock(MethodArgumentTypeMismatchException.class);

        var response = new GlobalExceptionHandler().handleMethodArgumentTypeMismatch(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("A request parameter has an invalid type.");
        verifyNoInteractions(exception);
    }

    @Test
    void invalidTypeDoesNotReflectRejectedValue() {
        var exception = mock(HttpMessageNotReadableException.class);
        String marker = "SECRET_TOKEN_DO_NOT_ECHO";
        when(exception.getCause()).thenReturn(InvalidFormatException.from(null, marker, marker, Integer.class));

        var response = new GlobalExceptionHandler().handleHttpMessageNotReadable(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("Integer").doesNotContain(marker);
    }
}
