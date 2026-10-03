package com.hs.hstesis.iam.infrastructure.authorization.sfs.configuration;

import com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline.TrustedOriginFilter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AzureCorsPolicyTest {
    private static final String ORIGIN = "https://thesis.example.azurewebsites.net";
    private WebSecurityConfiguration security;
    private MockMvc mvc;

    @RestController static class Controller {
        @PostMapping("/api/v1/write") public String write() {return "accepted";}
        @GetMapping("/api/v1/read") public String read() {return "accepted";}
    }

    @BeforeEach void configure() {
        // Only the actual CORS factory is used here; full authentication and
        // masked-CSRF behavior are covered separately by the security suite.
        security = new WebSecurityConfiguration(null, null, null, null, null);
        ReflectionTestUtils.setField(security, "allowedOrigins", List.of(ORIGIN));
        mvc = MockMvcBuilders.standaloneSetup(new Controller()).addFilters(
                new ForwardedHeaderFilter(), new CorsFilter(security.corsConfigurationSource()),
                new TrustedOriginFilter(Set.of(ORIGIN))).build();
    }

    @Test void sameOriginCookieWriteWorksBehindAzureHttpsProxy() throws Exception {
        mvc.perform(post("/api/v1/write").header("Origin", ORIGIN)
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "thesis.example.azurewebsites.net")
                .cookie(new Cookie("JWT_TOKEN", "synthetic-cookie")))
                .andExpect(status().isOk()).andExpect(content().string("accepted"));
    }

    @Test void trustedOriginWorksEvenWhenInternalServletOriginDiffers() throws Exception {
        mvc.perform(post("/api/v1/write").header("Origin", ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test void browserGetWithoutOriginHeaderIsAllowed() throws Exception {
        mvc.perform(get("/api/v1/read")).andExpect(status().isOk());
    }

    @Test void trustedPreflightPermitsRequiredWriteAndCsrfHeaders() throws Exception {
        mvc.perform(options("/api/v1/write").header("Origin", ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", ORIGIN));
    }

    @Test void untrustedAndOpaqueOriginsAreStillRejected() throws Exception {
        for (String origin : List.of("https://untrusted.example", "null")) {
            mvc.perform(post("/api/v1/write").header("Origin", origin))
                    .andExpect(status().isForbidden()).andExpect(content().string("Invalid CORS request"))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Test void cookieWriteWithoutOriginIsStillRejected() throws Exception {
        mvc.perform(post("/api/v1/write").cookie(new Cookie("JWT_TOKEN", "synthetic-cookie")))
                .andExpect(status().isForbidden());
    }

    @Test void invalidOriginConfigurationFailsBeforeHandlingRequests() {
        for (var origins : List.of(List.<String>of(), List.of("*"))) {
            ReflectionTestUtils.setField(security, "allowedOrigins", origins);
            assertThatThrownBy(security::corsConfigurationSource).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
