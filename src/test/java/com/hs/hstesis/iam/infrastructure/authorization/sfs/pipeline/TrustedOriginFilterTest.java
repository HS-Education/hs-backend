package com.hs.hstesis.iam.infrastructure.authorization.sfs.pipeline;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrustedOriginFilterTest {
    @Test void trustedCookieWriteIsAllowed() throws Exception { check("POST", "https://app.example", true, false, 200); }
    @Test void hostileOriginIsRejected() throws Exception { check("POST", "https://evil.invalid", true, false, 403); }
    @Test void missingCookieOriginIsRejected() throws Exception { check("POST", null, true, false, 403); }
    @Test void bearerClientWithoutOriginIsAllowed() throws Exception { check("POST", null, false, true, 200); }
    @Test void readOnlyNavigationDoesNotNeedOrigin() throws Exception { check("GET", null, true, false, 200); }
    private void check(String method, String origin, boolean cookie, boolean bearer, int status) throws Exception {
        var request = new MockHttpServletRequest(method, "/api/v1/test");
        if (origin != null) request.addHeader("Origin", origin);
        if (cookie) request.setCookies(new Cookie("JWT_TOKEN", "token"));
        if (bearer) request.addHeader("Authorization", "Bearer test");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        new TrustedOriginFilter(Set.of("https://app.example")).doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(status);
        verify(chain, times(status == 200 ? 1 : 0)).doFilter(request, response);
    }
}
