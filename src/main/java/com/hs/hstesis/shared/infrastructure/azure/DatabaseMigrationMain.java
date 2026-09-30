package com.hs.hstesis.shared.infrastructure.azure;

import org.flywaydb.core.Flyway;
import java.sql.DriverManager;

/** Run via PropertiesLauncher in CD, before switching application artifacts. */
public final class DatabaseMigrationMain {
    private DatabaseMigrationMain() {}
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--verify-package".equals(args[0])) {
            for (String name : new String[]{"V1__initial_schema.sql", "V2__document_processing_generation.sql"}) {
                try (var resource = DatabaseMigrationMain.class.getClassLoader().getResourceAsStream("db/migration/" + name)) {
                    if (resource == null || resource.readNBytes(10).length != 10) {
                        throw new IllegalStateException("The release package is missing a required migration");
                    }
                }
            }
            System.out.println("Release migration resources verified; no database connection was made.");
            return;
        }
        String url = required("MIGRATION_DATABASE_URL");
        String user = required("MIGRATION_DATABASE_USER");
        String password = required("MIGRATION_DATABASE_PASSWORD");
        String applicationPassword = required("MIGRATION_APP_PASSWORD");
        validateMigrationUrl(url);
        migrateAndBootstrap(url, user, password, applicationPassword);
    }
    static void validateMigrationUrl(String url) {
        if (!url.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException("An Azure PostgreSQL URL is required");
        var uri = java.net.URI.create(url.substring(5));
        if (uri.getHost() == null || !uri.getHost().endsWith(".postgres.database.azure.com")
                || uri.getUserInfo() != null || uri.getFragment() != null || uri.getRawQuery() == null) {
            throw new IllegalArgumentException("Migration requires the expected Azure PostgreSQL endpoint");
        }
        var options = new java.util.HashMap<String, String>();
        for (String option : uri.getRawQuery().split("&")) {
            var parts = option.split("=", 2);
            if (parts.length != 2 || options.put(parts[0], parts[1]) != null) throw new IllegalArgumentException("Invalid database options");
        }
        if (!"verify-full".equals(options.get("sslmode"))
                || !"org.postgresql.ssl.DefaultJavaSSLFactory".equals(options.get("sslfactory"))) {
            throw new IllegalArgumentException("Database certificate and hostname validation are required");
        }
    }
    static void migrateAndBootstrap(String url, String user, String password, String applicationPassword) throws Exception {
        if (applicationPassword == null || applicationPassword.length() < 16) {
            throw new IllegalArgumentException("Application database password must be strong");
        }
        var flyway = Flyway.configure(DatabaseMigrationMain.class.getClassLoader()).dataSource(url, user, password)
                .locations("classpath:db/migration").failOnMissingLocations(true)
                .baselineOnMigrate(false).cleanDisabled(true).load();
        if (flyway.info().all().length == 0) throw new IllegalStateException("The release contains no database migrations");
        var result = flyway.migrate();
        // Parameters remain data. PostgreSQL format(%L) safely quotes the password inside DDL.
        try (var connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            try (var binding = connection.prepareStatement("SELECT set_config('hs.app_password', ?, true)")) {
                binding.setString(1, applicationPassword);
                binding.execute();
            }
            try (var sql = connection.createStatement()) {
                sql.execute("""
                    DO $bootstrap$ BEGIN
                      IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'thesis_app') THEN
                        CREATE ROLE thesis_app LOGIN;
                      END IF;
                      EXECUTE format('ALTER ROLE thesis_app PASSWORD %L', current_setting('hs.app_password'));
                    END $bootstrap$;
                    GRANT USAGE ON SCHEMA public TO thesis_app;
                    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO thesis_app;
                    GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO thesis_app;
                    REVOKE ALL ON TABLE public.flyway_schema_history FROM thesis_app;
                    """);
            }
            connection.commit();
        }
        System.out.println("Database migrations applied: " + result.migrationsExecuted);
    }
    private static String required(String name) {
        var value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing migration setting: " + name);
        return value;
    }
}
