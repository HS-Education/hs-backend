package com.hs.hstesis.shared.infrastructure.azure;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DatabaseMigrationUrlTest {
    @Test void expectedAzureTlsConfigurationIsAccepted() {
        assertThatCode(() -> DatabaseMigrationMain.validateMigrationUrl(
                "jdbc:postgresql://example.postgres.database.azure.com:5432/hs_thesis?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory"))
                .doesNotThrowAnyException();
    }
    @Test void disabledOrNonvalidatingTlsAndArbitraryHostsAreRejected() {
        for (String url : new String[]{"jdbc:postgresql://example.postgres.database.azure.com:5432/hs_thesis?sslmode=require",
                "jdbc:postgresql://evil.invalid/db?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory",
                "jdbc:postgresql://example.postgres.database.azure.com/db?sslmode=verify-full&sslmode=disable&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory",
                "jdbc:postgresql://example.postgres.database.azure.com/db?sslmode=verify-full&sslfactory=org.postgresql.ssl.NonValidatingFactory"}) {
            assertThatThrownBy(() -> DatabaseMigrationMain.validateMigrationUrl(url)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
