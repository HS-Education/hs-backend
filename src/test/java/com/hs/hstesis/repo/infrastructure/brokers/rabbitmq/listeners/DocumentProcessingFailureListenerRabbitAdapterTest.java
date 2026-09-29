package com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.listeners;

import com.hs.hstesis.repo.application.internal.commandservices.DocumentStatusTransitionService;
import com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos.DocumentProcessingFailureMessage;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DocumentProcessingFailureListenerRabbitAdapterTest {
    @Test
    void failureMessageInvokesFailureTransitionWithoutForwardingWorkerDetails() {
        var statusTransitions = mock(DocumentStatusTransitionService.class);
        var listener = new DocumentProcessingFailureListenerRabbitAdapter(statusTransitions);

        listener.receive(new DocumentProcessingFailureMessage(73L, "DOCUMENT_INVALID"));

        verify(statusTransitions).markFailed(73L);
    }
}
