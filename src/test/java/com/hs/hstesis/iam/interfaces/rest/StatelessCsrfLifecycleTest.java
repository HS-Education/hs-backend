package com.hs.hstesis.iam.interfaces.rest;

import com.hs.hstesis.assessments.interfaces.acl.AssessmentsContextFacade;
import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.commands.CreateUserCommand;
import com.hs.hstesis.iam.domain.model.entity.RefreshToken;
import com.hs.hstesis.iam.domain.services.SignInCommandService;
import com.hs.hstesis.iam.domain.services.UserCommandService;
import com.hs.hstesis.iam.infrastructure.authorization.sfs.configuration.AuthCookiePolicy;
import com.hs.hstesis.iam.infrastructure.authorization.sfs.configuration.WebSecurityConfiguration;
import com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline.CustomAccessDeniedHandler;
import com.hs.hstesis.iam.infrastructure.hashing.bcrypt.BCryptHashingService;
import com.hs.hstesis.iam.infrastructure.tokens.jwt.BearerTokenService;
import com.hs.hstesis.iam.infrastructure.tokens.jwt.RefreshTokenService;
import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import com.hs.hstesis.repo.domain.services.ChatService;
import com.hs.hstesis.repo.interfaces.rest.ChatController;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real production filters and controllers; only domain services are synthetic.
// No Azure, database, provider traffic, real accounts or one-off filter removal.
@SpringJUnitConfig(StatelessCsrfLifecycleTest.Configuration.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "app.auth.allowed-origins=https://thesis.example.azurewebsites.net",
        "app.auth.cookie-secure=true", "app.auth.origin-check-enabled=true", "app.frontend.enabled=true"
})
class StatelessCsrfLifecycleTest {
    private static final String ORIGIN = "https://thesis.example.azurewebsites.net";
    private static final Cookie JWT = new Cookie("JWT_TOKEN", "synthetic-jwt");
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filters;
    @Autowired BearerTokenService tokens;
    @Autowired SignInCommandService signIn;
    @Autowired RefreshTokenService refresh;
    @Autowired ChatService chat;
    @Autowired IamContextFacade iam;
    @Autowired AssessmentsContextFacade assessments;
    private MockMvc mvc;

    @BeforeEach void configure() {
        reset(tokens, signIn, refresh, chat, iam, assessments);
        when(tokens.getBearerTokenFrom(any())).thenAnswer(invocation -> {
            var cookies = ((HttpServletRequest) invocation.getArgument(0)).getCookies();
            return cookies == null ? null : Arrays.stream(cookies)
                    .filter(cookie -> "JWT_TOKEN".equals(cookie.getName())).map(Cookie::getValue).findFirst().orElse(null);
        });
        when(tokens.validateToken("synthetic-jwt")).thenReturn(true);
        when(tokens.getUsernameFromToken("synthetic-jwt")).thenReturn("fixture");
        when(iam.getAuthenticatedUserId()).thenReturn(7L);
        doAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(3);
            Runnable done = invocation.getArgument(4);
            onToken.accept("Synthetic answer");
            done.run();
            return null;
        }).when(chat).streamMessageResponse(anyLong(), eq(7L), anyString(), any(), any(), any());
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filters).build();
    }

    @Test void statelessJwtRequestsKeepCsrfProtectionWithoutCreatingAnHttpSession() throws Exception {
        assertThat(filters.getFilterChains().getFirst().getFilters())
                .anyMatch(filter -> filter instanceof org.springframework.security.web.csrf.CsrfFilter);
        var result = mvc.perform(get("/api/v1/auth/me").cookie(JWT)).andExpect(status().isOk()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test void consecutiveWritesAndAuthenticatedReadsRetainTheSameCsrfCookie() throws Exception {
        var csrf = bootstrap();
        for (var method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
            mvc.perform(get("/api/v1/auth/me").cookie(JWT, csrf.cookie()))
                    .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
            mvc.perform(withCsrf(request(method, "/api/v1/test"), csrf))
                    .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
        }
    }

    @Test void threeRealChatStreamsThenLogoutWorkWithoutReloadOrBootstrapBetweenWrites() throws Exception {
        var csrf = bootstrap();
        mvc.perform(withCsrf(post("/api/v1/test"), csrf)).andExpect(status().isOk());
        for (int turn = 0; turn < 3; turn++) {
            mvc.perform(withCsrf(post("/api/v1/chat/sessions/12/stream"), csrf)
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
                    .content("{\"question\":\"synthetic greeting\"}"))
                    .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("event: done")));
        }
        verify(chat, times(3)).streamMessageResponse(eq(12L), eq(7L), anyString(), any(), any(), any());
        var logout = mvc.perform(withCsrf(post("/api/v1/auth/log-out"), csrf))
                .andExpect(status().isOk()).andReturn();
        assertCsrfDeletion(logout);
        verify(refresh).deleteByUserId(7L);
    }

    @Test void missingCsrfReturnsSafeJson403BeforeAnyChatCall() throws Exception {
        mvc.perform(post("/api/v1/chat/sessions/12/stream").cookie(JWT).header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"not executed\"}"))
                .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_MISSING"))
                .andExpect(jsonPath("$.timestamp").isString()).andExpect(header().string("Cache-Control", "no-store"));
        verifyNoInteractions(chat);
    }

    @Test void missingHeaderAndMismatchedCookieAre403Not500() throws Exception {
        var csrf = bootstrap();
        mvc.perform(post("/api/v1/test").cookie(JWT, csrf.cookie()).header("Origin", ORIGIN))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
        mvc.perform(post("/api/v1/test").cookie(JWT, new Cookie("XSRF-TOKEN", "different-cookie"))
                .header("Origin", ORIGIN).header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test void permissionDenialIsNotMisclassifiedAsCsrf() throws Exception {
        // Controller/method denial is handled by the real MVC advice; filter-level
        // CSRF failures use CustomAccessDeniedHandler's explicit retry codes.
        mvc.perform(withCsrf(post("/api/v1/test/forbidden"), bootstrap()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("CSRF_TOKEN_MISSING", "CSRF_TOKEN_INVALID"))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test void allUnsafeMethodsStillRequireCsrfAndUntrustedOriginsRemainRejected() throws Exception {
        for (var method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
            mvc.perform(request(method, "/api/v1/test").cookie(JWT).header("Origin", ORIGIN))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_MISSING"));
        }
        var csrf = bootstrap();
        mvc.perform(post("/api/v1/test").cookie(JWT, csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
    }

    @Test void bootstrapRemainsMaskedHttpOnlySecureStrictAndNotCached() throws Exception {
        var result = mvc.perform(get("/api/v1/auth/csrf").cookie(JWT))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
        var cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
        assertThat(result.getResponse().getContentAsString()).doesNotContain(cookie.getValue());
    }

    @Test void successfulSignInExplicitlyRotatesCsrfButRejectedSignInDoesNot() throws Exception {
        var user = fixtureUser();
        var token = new RefreshToken();
        token.setToken("synthetic-refresh");
        when(signIn.handle(any())).thenReturn(Optional.of(ImmutablePair.of(user, "synthetic-jwt")));
        when(refresh.createRefreshToken(7L)).thenReturn(token);
        var csrf = bootstrap();
        var request = withCsrf(post("/api/v1/auth/sign-in"), csrf).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"fixture\",\"password\":\"synthetic-password\"}");
        assertCsrfDeletion(mvc.perform(request).andExpect(status().isOk()).andReturn());
        when(signIn.handle(any())).thenReturn(Optional.empty());
        mvc.perform(withCsrf(post("/api/v1/auth/sign-in"), csrf).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"fixture\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test void successfulRefreshExplicitlyRotatesCsrf() throws Exception {
        var token = new RefreshToken();
        token.setUser(fixtureUser());
        when(refresh.getRefreshTokenFromCookie(any())).thenReturn("synthetic-refresh");
        when(refresh.findByToken("synthetic-refresh")).thenReturn(Optional.of(token));
        when(refresh.verifyExpiration(token)).thenReturn(token);
        when(tokens.generateTokenFromUser(any())).thenReturn("synthetic-jwt");
        assertCsrfDeletion(mvc.perform(withCsrf(post("/api/v1/auth/refresh-token"), bootstrap()))
                .andExpect(status().isOk()).andReturn());
    }

    private Token bootstrap() throws Exception {
        var response = mvc.perform(get("/api/v1/auth/csrf").cookie(JWT)).andExpect(status().isOk()).andReturn().getResponse();
        var masked = JsonMapper.builder().build().readTree(response.getContentAsString()).get("token").asText();
        return new Token(response.getCookie("XSRF-TOKEN"), masked);
    }
    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, Token csrf) {
        return request.cookie(JWT, csrf.cookie()).header("Origin", ORIGIN).header("X-XSRF-TOKEN", csrf.token());
    }
    private static void assertCsrfDeletion(MvcResult result) {
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anyMatch(header -> header.startsWith("XSRF-TOKEN=;") && header.contains("Max-Age=0")
                        && header.contains("Secure") && header.contains("HttpOnly") && header.contains("SameSite=Strict"));
    }
    private static User fixtureUser() {
        var user = new User(new CreateUserCommand("Synthetic User", "fixture", "synthetic-hash"));
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setTemporaryPassword(false);
        return user;
    }
    @Test void missingPublicRoutesAre404ButUnknownApisStillRequireAuthentication() throws Exception {
        for (String path : new String[]{"/homeaaa", "/swagger-ui.html", "/v3/api-docs", "/missing.js"}) {
            mvc.perform(get(path)).andExpect(status().isNotFound()).andExpect(jsonPath("status").value(404));
        }
        mvc.perform(get("/api/v1/diagnostic-missing")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/diagnostic-missing").cookie(JWT))
                .andExpect(status().isNotFound()).andExpect(jsonPath("status").value(404))
                .andExpect(forwardedUrl(null));
        mvc.perform(get("/sign-in")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    private record Token(Cookie cookie, String token) { }

    @RestController static class Writes {
        @RequestMapping(value = "/api/v1/test", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
        public String write() { return "accepted"; }
        @PreAuthorize("hasAuthority('ADMIN_ONLY')")
        @PostMapping("/api/v1/test/forbidden") public String forbidden() { return "not permitted"; }
    }
    @org.springframework.context.annotation.Configuration
    @EnableWebSecurity @EnableWebMvc
    @Import({WebSecurityConfiguration.class, AuthController.class, CsrfController.class, ChatController.class, Writes.class})
    static class Configuration implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
        @Bean com.hs.hstesis.shared.interfaces.rest.FrontendController frontend() {
            return new com.hs.hstesis.shared.interfaces.rest.FrontendController();
        }
        @Bean com.hs.hstesis.shared.interfaces.rest.advice.GlobalExceptionHandler errors() {
            return new com.hs.hstesis.shared.interfaces.rest.advice.GlobalExceptionHandler();
        }
        @Override public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/**").addResourceLocations("classpath:/route-test-fixtures/");
        }
        @Bean(name = "defaultUserDetailsService") UserDetailsService users() {
            return username -> new UserDetailsImpl(7L, "Synthetic User", username, "synthetic-hash", true,
                    List.of(new SimpleGrantedAuthority("REPOSITORY_READ")), List.of("COORDINATOR"));
        }
        @Bean BearerTokenService tokens() { return mock(BearerTokenService.class); }
        @Bean BCryptHashingService hashing() { return mock(BCryptHashingService.class); }
        @Bean AuthenticationEntryPoint unauthorized() { return (request, response, error) -> response.setStatus(401); }
        @Bean JsonMapper mapper() { return JsonMapper.builder().build(); }
        @Bean CustomAccessDeniedHandler denied(JsonMapper mapper) { return new CustomAccessDeniedHandler(mapper); }
        @Bean AuthCookiePolicy cookies() { return new AuthCookiePolicy(true, "Strict", 60, 7); }
        @Bean SignInCommandService signIn() { return mock(SignInCommandService.class); }
        @Bean RefreshTokenService refresh() { return mock(RefreshTokenService.class); }
        @Bean UserCommandService usersCommands() { return mock(UserCommandService.class); }
        @Bean ChatService chat() { return mock(ChatService.class); }
        @Bean IamContextFacade iam() { return mock(IamContextFacade.class); }
        @Bean AssessmentsContextFacade assessments() { return mock(AssessmentsContextFacade.class); }
    }
}
