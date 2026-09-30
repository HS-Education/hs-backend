package com.hs.hstesis.shared.infrastructure.azure;

import com.hs.hstesis.repo.infrastructure.configuration.MinioConfig;
import com.hs.hstesis.repo.infrastructure.configuration.RabbitMqConfig;
import com.hs.hstesis.notifications.infrastructure.brokers.rabbitmq.NotificationRealtimeRabbitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class EnvironmentIsolationTest {
    @Test void azureProvidersDoNotCreateLocalBrokerOrStorageBeans() {
        new ApplicationContextRunner().withUserConfiguration(MinioConfig.class, RabbitMqConfig.class,
                NotificationRealtimeRabbitConfig.class).withPropertyValues("app.storage.provider=azure-blob",
                "app.messaging.provider=service-bus").run(context -> {
            assertThat(context).doesNotHaveBean(io.minio.MinioClient.class);
            assertThat(context).doesNotHaveBean(RabbitMqConfig.class);
            assertThat(context).doesNotHaveBean(NotificationRealtimeRabbitConfig.class);
        });
    }
}
