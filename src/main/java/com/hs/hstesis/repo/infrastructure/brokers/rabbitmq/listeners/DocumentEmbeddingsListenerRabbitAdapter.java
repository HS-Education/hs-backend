package com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.listeners;

import com.hs.hstesis.repo.domain.model.commands.SaveDocumentEmbeddingsCommand;
import com.hs.hstesis.repo.domain.model.valueobjects.ChunkEmbeddingData;
import com.hs.hstesis.repo.domain.services.DocumentCommandService;
import com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos.DocumentEmbeddingsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentEmbeddingsListenerRabbitAdapter {

    private static final Logger log = LoggerFactory.getLogger(DocumentEmbeddingsListenerRabbitAdapter.class);
    private final DocumentCommandService documentCommandService;

    public DocumentEmbeddingsListenerRabbitAdapter(DocumentCommandService documentCommandService) {
        this.documentCommandService = documentCommandService;
    }

    @RabbitListener(queues = "embeddings_ready_queue")
    public void receiveEmbeddings(DocumentEmbeddingsMessage message) {
        log.info("Received {} vectors for Document ID: {}",
                message.chunks().size(), message.documentId());

        var chunkData = message.chunks().stream()
                .map(c -> new ChunkEmbeddingData(
                        c.pageNumber(),
                        c.chunkIndex(),
                        c.content(),
                        c.embedding()
                ))
                .toList();

        var command = new SaveDocumentEmbeddingsCommand(message.documentId(), chunkData);
        documentCommandService.handle(command);
    }
}
