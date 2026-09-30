package com.hs.hstesis.iam.interfaces.rest;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

class CsrfProtectionTest {
    private MockMvc mvc;
    @RestController static class WriteController {
        @RequestMapping(value = "/api/v1/test", method = {RequestMethod.POST, RequestMethod.PUT,
                RequestMethod.PATCH, RequestMethod.DELETE})
        public String write() { return "accepted"; }
    }
    @BeforeEach void configure() {
        var repository = new CookieCsrfTokenRepository();
        repository.setCookieCustomizer(cookie -> cookie.path("/").httpOnly(true).secure(true).sameSite("Strict"));
        mvc = MockMvcBuilders.standaloneSetup(new CsrfController(), new WriteController())
                .addFilters(new CsrfFilter(repository)).build();
    }
    @Test void bootstrapIsPublicMaskedAndNotCached() throws Exception {
        var result = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN")).andReturn();
        var cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
        assertThat(result.getResponse().getContentAsString()).doesNotContain(cookie.getValue());
    }
    @Test void missingTokensRejectAllUnsafeMethods() throws Exception {
        for (var method : java.util.List.of("POST", "PUT", "PATCH", "DELETE")) {
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), "/api/v1/test"))
                    .andExpect(status().isForbidden());
        }
    }
    @Test void cookieWithoutHeaderIsRejected() throws Exception {
        var cookie = mvc.perform(get("/api/v1/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        mvc.perform(post("/api/v1/test").cookie(cookie)).andExpect(status().isForbidden());
    }
    @Test void invalidHeaderIsRejected() throws Exception {
        var cookie = mvc.perform(get("/api/v1/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        mvc.perform(post("/api/v1/test").cookie(cookie).header("X-XSRF-TOKEN", "invalid"))
                .andExpect(status().isForbidden());
    }
    @Test void validMaskedTokenAllowsWriteButDifferentCookieDoesNot() throws Exception {
        var result = mvc.perform(get("/api/v1/auth/csrf")).andReturn();
        var token = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/v1/test").cookie(result.getResponse().getCookie("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/test").cookie(new Cookie("XSRF-TOKEN", "another-session"))
                .header("X-XSRF-TOKEN", token)).andExpect(status().isForbidden());
    }
}
