package com.hs.hstesis.shared.infrastructure.azure;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Never bootstrap users or sign tokens using an unresolved Key Vault reference. */
@Configuration
@Profile("azure")
public class AzureSecretsConfiguration {
    @Bean
    static BeanFactoryPostProcessor verifyAzureSecrets(Environment environment) {
        return factory -> {
            for (String name : new String[]{"JWT_SECRET", "WORKER_API_KEY", "POSTGRES_PASSWORD", "ADMIN_PASSWORD"}) {
                String value = environment.getProperty(name);
                if (value == null || value.isBlank() || value.startsWith("@Microsoft.KeyVault(")) {
                    throw new IllegalStateException("Azure secret must be resolved before startup: " + name);
                }
            }
        };
    }
}
