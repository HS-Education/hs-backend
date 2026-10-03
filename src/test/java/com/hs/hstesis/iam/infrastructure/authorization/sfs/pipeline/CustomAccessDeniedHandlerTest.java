package com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

class CustomAccessDeniedHandlerTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(mapper);

    @Test void serializesDatesAndNeverLeaksCsrfValuesOrExceptionDetails() throws Exception {
        for (var denied : java.util.List.of(new MissingCsrfTokenException("sensitive-missing-token"),
                new InvalidCsrfTokenException(new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "sensitive-cookie"), "sensitive-header"))) {
            var response = new MockHttpServletResponse();
            handler.handle(new MockHttpServletRequest("POST", "/api/v1/chat/sessions/1/stream"), response, denied);
            assertThat(response.getStatus()).isEqualTo(403);
            var json = mapper.readTree(response.getContentAsString());
            assertThat(json.get("timestamp").isTextual()).isTrue();
            assertThat(json.get("code").asText()).startsWith("CSRF_TOKEN_");
            assertThat(response.getContentAsString()).doesNotContain("sensitive-");
        }
    }

    @Test void anOrdinaryPermissionFailureCannotAuthorizeCsrfReplay() throws Exception {
        var response = new MockHttpServletResponse();
        handler.handle(new MockHttpServletRequest("POST", "/api/v1/users"), response,
                new AccessDeniedException("sensitive-permission-details"));
        assertThat(mapper.readTree(response.getContentAsString()).get("code").asText()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getContentAsString()).contains("Solo el administrador").doesNotContain("sensitive-");
    }

    @Test void doesNotWriteJsonOrChangeTheStatusOfAnAlreadyCommittedStream() throws Exception {
        var response = new MockHttpServletResponse();
        response.setContentType("text/event-stream");
        response.getWriter().write("event: token\ndata: {}\n\n");
        response.flushBuffer();
        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("disconnected"));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo("event: token\ndata: {}\n\n");
    }
}
