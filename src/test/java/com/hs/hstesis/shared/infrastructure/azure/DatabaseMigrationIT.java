package com.hs.hstesis.shared.infrastructure.azure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class DatabaseMigrationIT {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Test void freshDatabaseMigratesAndRepeatedExecutionIsIdempotent() throws Exception {
        assertThat(DatabaseMigrationIT.class.getResource("/db/migration/V1__initial_schema.sql")).isNotNull();
        var flyway = Flyway.configure(DatabaseMigrationIT.class.getClassLoader())
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").failOnMissingLocations(true).cleanDisabled(true).baselineOnMigrate(false).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        // Validate every mapped entity, not only the document/vector tables.
        var factory = new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        factory.setPackagesToScan("com.hs.hstesis");
        factory.setJpaVendorAdapter(new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(java.util.Map.of("hibernate.hbm2ddl.auto", "validate",
                "hibernate.physical_naming_strategy", "com.hs.hstesis.shared.infrastructure.persistance.jpa.strategy.SnakeCasePhysicalNamingStrategy"));
        factory.afterPropertiesSet();
        factory.destroy();
        String appPassword = "synthetic'quoted-application-password";
        DatabaseMigrationMain.migrateAndBootstrap(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), appPassword);
        try (var application = DriverManager.getConnection(postgres.getJdbcUrl(), "thesis_app", appPassword)) {
            try (var sql = application.createStatement(); var rows = sql.executeQuery("SELECT count(*) FROM documents")) {
                assertThat(rows.next()).isTrue();
            }
            assertThatThrownBy(() -> application.createStatement().execute("CREATE TABLE unauthorized_table (id int)"))
                    .isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> application.createStatement().execute("SELECT * FROM flyway_schema_history"))
                    .isInstanceOf(java.sql.SQLException.class);
        }
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var sql = connection.createStatement()) {
            try (var rows = sql.executeQuery("SELECT extname FROM pg_extension WHERE extname='vector'")) {
                assertThat(rows.next()).isTrue();
            }
            try (var rows = sql.executeQuery("SELECT column_default, is_nullable FROM information_schema.columns WHERE table_name='documents' AND column_name='processing_generation'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("1");
                assertThat(rows.getString(2)).isEqualTo("NO");
            }
            try (var rows = sql.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isGreaterThan(20);
            }
        }
    }
}
