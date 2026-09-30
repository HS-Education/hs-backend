package com.hs.hstesis.shared.infrastructure.azure;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("azure")
public class AzureClientConfiguration {
    @Bean
    public TokenCredential azureCredential() { return new ManagedIdentityCredentialBuilder().build(); }

    @Bean
    public BlobServiceClient blobServiceClient(TokenCredential credential,
            @Value("${azure.storage.account-url}") String endpoint) {
        java.net.URI uri = java.net.URI.create(endpoint);
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null
                || !uri.getHost().endsWith(".blob.core.windows.net") || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))) {
            throw new IllegalArgumentException("An Azure HTTPS storage endpoint is required");
        }
        return new BlobServiceClientBuilder().endpoint(endpoint).credential(credential).buildClient();
    }

    @Bean
    public ServiceBusClientBuilder serviceBusClientBuilder(TokenCredential credential,
            @Value("${azure.servicebus.namespace}") String namespace) {
        if (!namespace.endsWith(".servicebus.windows.net") || namespace.contains("/") || namespace.contains(":")) {
            throw new IllegalArgumentException("An Azure Service Bus namespace is required");
        }
        return new ServiceBusClientBuilder().credential(namespace, credential);
    }
}
