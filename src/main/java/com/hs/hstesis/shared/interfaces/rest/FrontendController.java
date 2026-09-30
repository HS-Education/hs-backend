package com.hs.hstesis.shared.interfaces.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Optional same-origin SPA hosting; REST APIs never pass through this fallback. */
@Controller
@ConditionalOnProperty(name = "app.frontend.enabled", havingValue = "true")
public class FrontendController {
    @GetMapping({"/", "/sign-in", "/update-password", "/home", "/classrooms", "/classrooms/{id}",
            "/classrooms/{id}/quizzes/{quizId}", "/repository", "/metrics", "/help", "/preferences",
            "/notifications", "/chat", "/unauthorized", "/server-error", "/not-found", "/admin/**"})
    public String index() { return "forward:/index.html"; }
}
