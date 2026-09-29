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
        var emitter = new SseEmitter(0L);
        var userEmitters = emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArraySet<>());
        userEmitters.add(emitter);

        Runnable remove = () -> remove(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ignored -> remove.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("connected", true)));
        } catch (IOException exception) {
            remove.run();
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    public void publish(NotificationRealtimeMessage message) {
        var userEmitters = emittersByUser.get(message.userId());
        if (userEmitters == null) return;

        for (var emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(message.id()))
                        .name("notification")
                        .data(message));
            } catch (IOException exception) {
                emitter.completeWithError(exception);
                remove(message.userId(), emitter);
            }
        }
    }

    @Scheduled(fixedRate = 30_000)
    void sendKeepAlive() {
        emittersByUser.forEach((userId, userEmitters) -> {
            for (var emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("keep-alive"));
                } catch (IOException exception) {
                    emitter.completeWithError(exception);
                    remove(userId, emitter);
                }
            }
        });
    }

    private void remove(Long userId, SseEmitter emitter) {
        var userEmitters = emittersByUser.get(userId);
        if (userEmitters == null) return;
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) emittersByUser.remove(userId, userEmitters);
    }
}
