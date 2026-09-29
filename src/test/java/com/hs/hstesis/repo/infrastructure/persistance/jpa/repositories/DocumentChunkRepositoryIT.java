package com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class DocumentChunkRepositoryIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    private Connection connect() throws Exception {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @BeforeEach
    void resetSchema() throws Exception {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS document_chunks, document_targets, documents CASCADE");
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("db/init/01_init.sql"));
        }
    }

    @Test
    void cosineSearchReturnsOnlyTheAllowedTarget() throws Exception {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            insert(connection, 1, "allowed lesson", 10, "SECOND", vector(1, 0));
            insert(connection, 2, "TEACHER_ONLY_CANARY", 10, "THIRD", vector(1, 0));
            insert(connection, 3, "other course", 99, "SECOND", vector(0, 1));
            String sql = """
                    SELECT c.content, 1 - (c.embedding <=> cast('%s' as vector)) AS similarity
                    FROM document_chunks c
                    JOIN documents d ON c.document_id = d.id
                    JOIN document_targets t ON d.id = t.document_id
                    WHERE t.course_id = 10 AND t.education_level = 'SECONDARY' AND t.grade_level = 'SECOND'
                    ORDER BY c.embedding <=> cast('%s' as vector)
                    LIMIT 10
                    """.formatted(vector(1, 0), vector(1, 0));
            try (ResultSet rows = statement.executeQuery(sql)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("content")).isEqualTo("allowed lesson");
                assertThat(rows.getDouble("similarity")).isCloseTo(1, org.assertj.core.data.Offset.offset(0.00001));
                assertThat(rows.next()).isFalse();
            }
        }
    }

    @Test
    void rollbackIsVisibleFromAFreshConnection() throws Exception {
        try (Connection transaction = connect()) {
            transaction.setAutoCommit(false);
            insert(transaction, 4, "uncommitted", 10, "SECOND", vector(1, 0));
            transaction.rollback();
        }
        try (Connection fresh = connect(); Statement statement = fresh.createStatement();
             ResultSet rows = statement.executeQuery("SELECT count(*) FROM documents")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isZero();
        }
    }

    @Test
    void hostileGradeValueIsBoundAsDataAndDoesNotBypassTargetFilter() throws Exception {
        try (Connection connection = connect()) {
            insert(connection, 7, "ALLOWED", 10, "SECOND", vector(1, 0));
            insert(connection, 8, "OTHER_GRADE_CANARY", 10, "THIRD", vector(1, 0));
            String sql = """
                    SELECT c.content FROM document_chunks c
                    JOIN documents d ON c.document_id = d.id
                    JOIN document_targets t ON d.id = t.document_id
                    WHERE t.course_id = ? AND t.education_level = ? AND t.grade_level = ?
                    AND d.status = 'READY'
                    ORDER BY c.embedding <=> cast(? as vector) LIMIT ?
                    """;
            try (var query = connection.prepareStatement(sql)) {
                query.setLong(1, 10L);
                query.setString(2, "SECONDARY");
                query.setString(3, "SECOND' OR '1'='1");
                query.setString(4, vector(1, 0));
                query.setInt(5, 10);
                try (ResultSet rows = query.executeQuery()) {
                    assertThat(rows.next()).isFalse();
                }
                query.setString(3, "SECOND");
                try (ResultSet rows = query.executeQuery()) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("ALLOWED");
                    assertThat(rows.next()).isFalse();
                }
            }
        }
    }

    @Test
    void onlyReadyDocumentsCanBeUsedAsRetrievedContext() throws Exception {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            insert(connection, 9, "READY_SOURCE", 10, "SECOND", vector(1, 0));
            insert(connection, 10, "PROCESSING_CANARY", 10, "SECOND", vector(1, 0));
            statement.executeUpdate("UPDATE documents SET status='PROCESSING' WHERE id=10");
            try (var query = connection.prepareStatement("""
                    SELECT c.content FROM document_chunks c
                    JOIN documents d ON c.document_id = d.id
                    JOIN document_targets t ON d.id = t.document_id
                    WHERE t.course_id = 10 AND d.status='READY'
                    ORDER BY c.embedding <=> cast(? as vector)
                    """)) {
                query.setString(1, vector(1, 0));
                try (ResultSet rows = query.executeQuery()) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString(1)).isEqualTo("READY_SOURCE");
                    assertThat(rows.next()).isFalse();
                }
            }
        }
    }

    private static void insert(Connection connection, long id, String content, long courseId,
                               String grade, String vector) throws Exception {
        try (var statement = connection.prepareStatement("""
                INSERT INTO documents (id, created_at, updated_at, title, author_id, topic_id, type, format,
                                       status, original_file_name, object_key, file_checksum)
                VALUES (?, now(), now(), ?, 1, 1, 'LESSON', 'PDF', 'READY', ?, ?, ?)
                """)) {
            statement.setLong(1, id);
            statement.setString(2, content);
            statement.setString(3, "file-" + id);
            statement.setString(4, "object-" + id);
            statement.setString(5, "checksum-" + id);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("""
                INSERT INTO document_targets (document_id, education_level, grade_level, course_id)
                VALUES (?, 'SECONDARY', ?, ?)
                """)) {
            statement.setLong(1, id);
            statement.setString(2, grade);
            statement.setLong(3, courseId);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("""
                INSERT INTO document_chunks (document_id, content, page_number, chunk_index, embedding)
                VALUES (?, ?, 1, 1, cast(? as vector))
                """)) {
            statement.setLong(1, id);
            statement.setString(2, content);
            statement.setString(3, vector);
            statement.executeUpdate();
        }
    }

    private static String vector(float x, float y) {
        float[] values = new float[1024];
        values[0] = x;
        values[1] = y;
        return Arrays.toString(values).replace(" ", "");
    }
}
