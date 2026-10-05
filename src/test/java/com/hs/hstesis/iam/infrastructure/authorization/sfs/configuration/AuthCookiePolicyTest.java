package com.hs.hstesis.iam.infrastructure.authorization.sfs.configuration;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AuthCookiePolicyTest {
    @Test void localCookiesRemainCompatible() {
        var policy = new AuthCookiePolicy(false, "Strict", 60, 7);
        assertThat(policy.access("token").isHttpOnly()).isTrue();
        assertThat(policy.access("token").isSecure()).isFalse();
        assertThat(policy.refresh("token").getMaxAge().toSeconds()).isEqualTo(604800);
        assertThat(policy.refresh("token").getPath()).isEqualTo("/api/v1/auth/refresh-token");
    }
    @Test void cloudCookiesAndDeletionUseTheSameSecurityAttributes() {
        var policy = new AuthCookiePolicy(true, "Strict", 60, 7);
        assertThat(policy.access("token").isSecure()).isTrue();
        assertThat(policy.clearAccess().getMaxAge().toSeconds()).isZero();
        assertThat(policy.clearRefresh().getPath()).isEqualTo(policy.refresh("x").getPath());
        assertThat(policy.clearAccess().getSameSite()).isEqualTo("Strict");
        assertThat(policy.clearCsrf().getName()).isEqualTo("XSRF-TOKEN");
        assertThat(policy.clearCsrf().getPath()).isEqualTo("/");
        assertThat(policy.clearCsrf().getMaxAge().toSeconds()).isZero();
        assertThat(policy.clearCsrf().isHttpOnly()).isTrue();
        assertThat(policy.clearCsrf().isSecure()).isTrue();
        assertThat(policy.clearCsrf().getSameSite()).isEqualTo("Strict");
    }
    @Test void crossSiteCookiesCannotDisableTls() {
        assertThatThrownBy(() -> new AuthCookiePolicy(false, "None", 60, 7)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthCookiePolicy(true, "anything", 60, 7)).isInstanceOf(IllegalArgumentException.class);
    }
}
