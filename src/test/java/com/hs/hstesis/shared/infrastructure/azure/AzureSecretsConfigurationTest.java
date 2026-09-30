package com.hs.hstesis.shared.infrastructure.azure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import static org.assertj.core.api.Assertions.*;

class AzureSecretsConfigurationTest {
    private MockEnvironment complete() {
        var environment = new MockEnvironment();
        for (String name : new String[]{"JWT_SECRET", "WORKER_API_KEY", "POSTGRES_PASSWORD", "ADMIN_PASSWORD"})
            environment.setProperty(name, "synthetic-resolved-private-value");
        return environment;
    }
    @Test void resolvedSecretsAreAccepted() {
        assertThatCode(() -> AzureSecretsConfiguration.verifyAzureSecrets(complete())
                .postProcessBeanFactory(new DefaultListableBeanFactory())).doesNotThrowAnyException();
    }
    @Test void unresolvedVaultReferenceCannotBootstrapAnAdministrator() {
        var environment = complete().withProperty("ADMIN_PASSWORD", "@Microsoft.KeyVault(SecretUri=https://example/secrets/admin)");
        assertThatThrownBy(() -> AzureSecretsConfiguration.verifyAzureSecrets(environment)
                .postProcessBeanFactory(new DefaultListableBeanFactory()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ADMIN_PASSWORD")
                .hasMessageNotContaining("SecretUri");
    }
}
