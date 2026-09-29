package com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.listeners;

import com.hs.hstesis.repo.application.internal.commandservices.DocumentStatusTransitionService;
import com.hs.hstesis.repo.domain.model.aggregates.Document;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentFormat;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentStatus;
import com.hs.hstesis.repo.domain.model.valueobjects.DocumentType;
import com.hs.hstesis.repo.infrastructure.configuration.RabbitMqConfig;
import com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos.DocumentProcessingFailureMessage;
import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in infrastructure test: a real RabbitMQ failure message must persist FAILED in PostgreSQL. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DocumentStatusTransitionService.class, RabbitMqConfig.class})
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentProcessingFailureFlowIT {
    private static final String FAILURE_QUEUE = "document_processing_failed_queue";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-alpine")
            .withAdminUser("quality_gate")
            .withAdminPassword("synthetic-quality-gate-password");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DocumentRepository documents;

    @Autowired
    private DocumentStatusTransitionService statusTransitions;

    @Autowired
    private MessageConverter messageConverter;

    private CachingConnectionFactory connectionFactory;
    private RabbitTemplate rabbitTemplate;
    private SimpleMessageListenerContainer listenerContainer;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        // Dynamic properties can be requested before the JUnit Testcontainers callback runs.
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeAll
    void initializeSchema() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("db/init/01_init.sql"));
        }
    }

    @BeforeEach
    void startRabbitConsumer() {
        connectionFactory = new CachingConnectionFactory(rabbit.getHost(), rabbit.getAmqpPort());
        connectionFactory.setUsername("quality_gate");
        connectionFactory.setPassword("synthetic-quality-gate-password");

        var rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.declareQueue(new Queue(FAILURE_QUEUE, true));

        rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        var listener = new DocumentProcessingFailureListenerRabbitAdapter(statusTransitions);
        var listenerAdapter = new MessageListenerAdapter(listener, "receive");
        listenerAdapter.setMessageConverter(messageConverter);
        listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        listenerContainer.setQueueNames(FAILURE_QUEUE);
        listenerContainer.setMessageListener(listenerAdapter);
        listenerContainer.start();
    }

    @AfterEach
    void stopRabbitConsumer() {
        if (listenerContainer != null) listenerContainer.stop();
        if (connectionFactory != null) connectionFactory.destroy();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void corruptDocumentFailureTravelsThroughRabbitAndPersistsFailedStatus() throws Exception {
        var document = documents.saveAndFlush(new Document(
                "Synthetic corrupt PDF", 41L, 9L, DocumentType.ACADEMIC, DocumentFormat.PDF,
                "corrupt.pdf", "quality-gate/corrupt-41.pdf", "synthetic-checksum-41"));
        document.markAsProcessing();
        document = documents.saveAndFlush(document);
        Long documentId = document.getId();

        rabbitTemplate.convertAndSend(FAILURE_QUEUE,
                new DocumentProcessingFailureMessage(documentId, "DOCUMENT_INVALID"));

        awaitStatus(documentId, DocumentStatus.FAILED, Duration.ofSeconds(10));
        assertThat(documents.findById(documentId)).get()
                .extracting(Document::getStatus)
                .isEqualTo(DocumentStatus.FAILED);
    }

    private void awaitStatus(Long documentId, DocumentStatus expected, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        Optional<Document> latest = Optional.empty();
        while (System.nanoTime() < deadline) {
            latest = documents.findById(documentId);
            if (latest.map(Document::getStatus).filter(expected::equals).isPresent()) return;
            Thread.sleep(50);
        }
        assertThat(latest).get().extracting(Document::getStatus).isEqualTo(expected);
    }
}
