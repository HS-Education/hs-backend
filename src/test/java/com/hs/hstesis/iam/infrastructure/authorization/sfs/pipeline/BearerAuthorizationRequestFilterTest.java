package com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline;

import com.hs.hstesis.iam.infrastructure.tokens.jwt.BearerTokenService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BearerAuthorizationRequestFilterTest {
    private final BearerTokenService tokens = mock(BearerTokenService.class);
    private final UserDetailsService users = mock(UserDetailsService.class);
    private final BearerAuthorizationRequestFilter bearer = new BearerAuthorizationRequestFilter(tokens, users);
    private final SecurityContextHolderFilter contexts = new SecurityContextHolderFilter(
            new RequestAttributeSecurityContextRepository());
    private final AuthorizationFilter authorization = new AuthorizationFilter(
            AuthenticatedAuthorizationManager.authenticated());

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void authenticatedStreamKeepsIdentityOnAsyncAndErrorDispatchAfterResponseIsCommitted() throws Exception {
        var request = request();
        var response = new MockHttpServletResponse();
        when(tokens.getBearerTokenFrom(request)).thenReturn("valid");
        when(tokens.validateToken("valid")).thenReturn(true);
        when(tokens.getUsernameFromToken("valid")).thenReturn("teacher");
        when(users.loadUserByUsername("teacher")).thenReturn(
                User.withUsername("teacher").password("unused").roles("TEACHER").build());
        FilterChain endpoint = (req, res) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("teacher");
            res.flushBuffer();
        };
        dispatch(request, response, endpoint);
        assertThat(response.isCommitted()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        for (var type : new DispatcherType[] {DispatcherType.ASYNC, DispatcherType.ERROR}) {
            request.setDispatcherType(type);
            if (type == DispatcherType.ERROR) {
                request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
            }
            dispatch(request, response, endpoint);
        }
        verify(users, times(1)).loadUserByUsername("teacher");
        assertThat(request.getSession(false)).isNull();
    }

    @Test void missingOrInvalidTokenCannotOpenStream() {
        for (String token : new String[] {null, "invalid"}) {
            var request = request();
            when(tokens.getBearerTokenFrom(request)).thenReturn(token);
            FilterChain endpoint = mock(FilterChain.class);
            assertThatThrownBy(() -> dispatch(request, new MockHttpServletResponse(), endpoint))
                    .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
            verifyNoInteractions(endpoint);
        }
    }

    @Test void asyncDispatchWithoutAuthenticatedInitialRequestCannotBypassSecurity() {
        var request = request();
        request.setDispatcherType(DispatcherType.ASYNC);
        FilterChain endpoint = mock(FilterChain.class);
        assertThatThrownBy(() -> dispatch(request, new MockHttpServletResponse(), endpoint))
                .isInstanceOf(org.springframework.security.core.AuthenticationException.class);
        verifyNoInteractions(endpoint);
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("GET", "/api/v1/notifications/stream");
        request.setAsyncSupported(true);
        return request;
    }

    private void dispatch(MockHttpServletRequest request, MockHttpServletResponse response, FilterChain endpoint)
            throws Exception {
        contexts.doFilter(request, response, (req, res) ->
                bearer.doFilter(req, res, (authenticatedRequest, authenticatedResponse) ->
                        authorization.doFilter(authenticatedRequest, authenticatedResponse, endpoint)));
    }
}
