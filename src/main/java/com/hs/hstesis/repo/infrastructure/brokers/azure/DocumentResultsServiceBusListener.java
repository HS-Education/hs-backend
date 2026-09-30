package com.hs.hstesis.repo.infrastructure.brokers.azure;

import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "app.messaging.provider", havingValue = "service-bus")
public class DocumentResultsServiceBusListener implements SmartLifecycle {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DocumentResultsServiceBusListener.class);
    private final List<ServiceBusProcessorClient> processors;
    private volatile boolean running;
    public DocumentResultsServiceBusListener(ServiceBusClientBuilder builder, AzureDocumentResultHandler handler,
            @Value("${azure.servicebus.results-queue}") String resultsQueue,
            @Value("${azure.servicebus.failures-queue}") String failuresQueue,
            @Value("${azure.servicebus.max-lock-renewal-minutes:30}") long renewalMinutes) {
        processors = List.of(processor(builder, resultsQueue, renewalMinutes,
                        data -> handler.accept(AzureJson.read(data, EmbeddingsReference.class))),
                processor(builder, failuresQueue, renewalMinutes,
                        data -> handler.fail(AzureJson.read(data, AzureDocumentResultHandler.Failure.class))));
    }
    private ServiceBusProcessorClient processor(ServiceBusClientBuilder builder, String queue, long renewalMinutes, Consumer<byte[]> handler) {
        return builder.processor().queueName(queue).disableAutoComplete().maxConcurrentCalls(1).prefetchCount(0)
                .maxAutoLockRenewDuration(Duration.ofMinutes(renewalMinutes))
                .processMessage(context -> {
                    try {
                        byte[] data = context.getMessage().getBody().toBytes();
                        if (data.length > 16 * 1024) throw new IllegalArgumentException("Message exceeds reference limit");
                        handler.accept(data);
                        context.complete(); // Handler's database transaction has committed before settlement.
                    } catch (IllegalArgumentException invalid) {
                        context.deadLetter(new DeadLetterOptions().setDeadLetterReason("INVALID_CONTRACT"));
                    } catch (RuntimeException unavailable) {
                        LOG.warn("Azure result processing will retry; errorType={}", unavailable.getClass().getSimpleName());
                        context.abandon();
                    }
                }).processError(context -> LOG.warn("Service Bus processor error; errorType={}", context.getException().getClass().getSimpleName()))
                .buildProcessorClient();
    }
    @Override public void start() { processors.forEach(ServiceBusProcessorClient::start); running = true; }
    @Override public void stop() { processors.forEach(ServiceBusProcessorClient::close); running = false; }
    @Override public boolean isRunning() { return running; }
}
