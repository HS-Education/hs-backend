package com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline;

import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper objectMapper;

    public CustomAccessDeniedHandler(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        // An SSE connection can already be committed when its client disconnects.
        if (response.isCommitted()) return;
        String message = "No tienes permisos para acceder a este recurso.";
        String code = "ACCESS_DENIED";

        if (accessDeniedException instanceof CsrfException) {
            code = accessDeniedException instanceof MissingCsrfTokenException
                    ? "CSRF_TOKEN_MISSING" : "CSRF_TOKEN_INVALID";
            message = "La protección de la solicitud debe renovarse. Inténtalo nuevamente.";
        }

        if (!(accessDeniedException instanceof CsrfException) && "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().endsWith("/api/v1/users")) {
            message = "Solo el administrador del sistema puede registrar nuevos usuarios.";
        }

        DeniedResource apiErrorResponse = new DeniedResource(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                message, LocalDateTime.now(), code
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(objectMapper.writeValueAsString(apiErrorResponse));
    }

    public record DeniedResource(int status, String error, String message, LocalDateTime timestamp, String code) { }
}
