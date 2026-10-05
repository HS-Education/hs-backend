package com.hs.hstesis.notifications.infrastructure.sse;

import com.hs.hstesis.notifications.infrastructure.brokers.rabbitmq.dtos.NotificationRealtimeMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class NotificationSseRegistry {

    private final Map<Long, CopyOnWriteArraySet<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        var emitter = newEmitter();
        emittersByUser.compute(userId, (ignored, existing) -> {
            var userEmitters = existing == null ? new CopyOnWriteArraySet<SseEmitter>() : existing;
            userEmitters.add(emitter);
            return userEmitters;
        });

        Runnable remove = () -> remove(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ignored -> remove.run());

        send(userId, emitter, SseEmitter.event()
                    .name("connected")
                    .data(Map.of("connected", true)));

        return emitter;
    }

    public void publish(NotificationRealtimeMessage message) {
        var userEmitters = emittersByUser.get(message.userId());
        if (userEmitters == null) return;

        for (var emitter : userEmitters) {
            send(message.userId(), emitter, SseEmitter.event()
                        .id(String.valueOf(message.id()))
                        .name("notification")
                        .data(message));
        }
    }

    @Scheduled(fixedRate = 30_000)
    void sendKeepAlive() {
        emittersByUser.forEach((userId, userEmitters) -> {
            for (var emitter : userEmitters) {
                send(userId, emitter, SseEmitter.event().comment("keep-alive"));
            }
        });
    }

    private void remove(Long userId, SseEmitter emitter) {
        emittersByUser.computeIfPresent(userId, (ignored, userEmitters) -> {
            userEmitters.remove(emitter);
            return userEmitters.isEmpty() ? null : userEmitters;
        });
    }

    SseEmitter newEmitter() { return new SseEmitter(0L); }

    private void send(Long userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            // Remove first: a completed/failed servlet AsyncContext cannot be reused.
            remove(userId, emitter);
            try { emitter.completeWithError(exception); }
            catch (IllegalStateException ignored) { /* The container already ended the connection. */ }
        }
    }
}
