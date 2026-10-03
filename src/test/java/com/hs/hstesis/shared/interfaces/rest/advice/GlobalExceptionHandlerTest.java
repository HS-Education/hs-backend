package com.hs.hstesis.shared.interfaces.rest.advice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
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
