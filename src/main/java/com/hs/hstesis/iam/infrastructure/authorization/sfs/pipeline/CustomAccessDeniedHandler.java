package com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.hstesis.shared.interfaces.rest.resources.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomAccessDeniedHandler() {
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        String message = "No tienes permisos para acceder a este recurso.";

        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().endsWith("/api/v1/users")) {
            message = "Solo el administrador del sistema puede registrar nuevos usuarios.";
        }

        ApiErrorResponse apiErrorResponse = new ApiErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                message
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiErrorResponse));
    }
}
