package com.hs.hstesis.notifications.infrastructure.sse;

import com.hs.hstesis.notifications.infrastructure.brokers.rabbitmq.dtos.NotificationRealtimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationSseRegistryTest {
    @Test void failedInitialConnectionIsRemovedEvenIfCompletionAlreadyFailed() throws Exception {
        var registry = spy(new NotificationSseRegistry());
        var emitter = mock(SseEmitter.class);
        doReturn(emitter).when(registry).newEmitter();
        doThrow(new IllegalStateException("Synthetic closed context")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        doThrow(new IllegalStateException("Already complete")).when(emitter).completeWithError(any());
        assertThatCode(() -> registry.subscribe(7L)).doesNotThrowAnyException();
        registry.sendKeepAlive();
        registry.publish(message());
        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test void disconnectedKeepAliveDoesNotPreventHealthySubscribersReceivingEvents() throws Exception {
        var registry = spy(new NotificationSseRegistry());
        var disconnected = mock(SseEmitter.class);
        var healthy = mock(SseEmitter.class);
        doReturn(disconnected, healthy).when(registry).newEmitter();
        registry.subscribe(7L);
        registry.subscribe(7L);
        doThrow(new IllegalStateException("Synthetic disconnected context")).when(disconnected).send(any(SseEmitter.SseEventBuilder.class));
        registry.sendKeepAlive();
        registry.sendKeepAlive();
        registry.publish(message());
        verify(disconnected, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(healthy, times(4)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test void brokenPipeWhilePublishingRemovesSubscriberBeforeTheNextHeartbeat() throws Exception {
        var registry = spy(new NotificationSseRegistry());
        var emitter = mock(SseEmitter.class);
        doReturn(emitter).when(registry).newEmitter();
        registry.subscribe(7L);
        doThrow(new IOException("Synthetic broken pipe")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        registry.publish(message());
        registry.sendKeepAlive();
        registry.publish(message());
        verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    }

    private NotificationRealtimeMessage message() {
        return new NotificationRealtimeMessage(1L, 7L, "Synthetic notice", null, false, new java.util.Date());
    }
}
