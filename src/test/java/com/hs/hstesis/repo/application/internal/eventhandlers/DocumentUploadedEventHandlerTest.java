package com.hs.hstesis.repo.application.internal.eventhandlers;

import com.hs.hstesis.repo.application.internal.commandservices.DocumentStatusTransitionService;
import com.hs.hstesis.repo.application.internal.outboundservices.messaging.DocumentProcessingPublisher;
import com.hs.hstesis.repo.domain.model.events.DocumentUploadedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class DocumentUploadedEventHandlerTest {
    private final DocumentProcessingPublisher publisher = mock(DocumentProcessingPublisher.class);
    private final DocumentStatusTransitionService transitions = mock(DocumentStatusTransitionService.class);
    private final DocumentUploadedEventHandler handler = new DocumentUploadedEventHandler(publisher, transitions);
    private final DocumentUploadedEvent event = new DocumentUploadedEvent(this, 17L, "object-17");

    @Test
    void processingStateIsCommittedBeforePublishingToFastWorker() {
        handler.on(event);

        var order = inOrder(transitions, publisher);
        order.verify(transitions).markProcessing(17L);
        order.verify(publisher).publish(event);
        verify(transitions, never()).markFailed(anyLong());
    }

    @Test
    void brokerFailureMarksDocumentFailed() {
        doThrow(new RuntimeException("broker down")).when(publisher).publish(event);

        handler.on(event);

        var order = inOrder(transitions, publisher);
        order.verify(transitions).markProcessing(17L);
        order.verify(publisher).publish(event);
        order.verify(transitions).markFailed(17L);
    }
}
