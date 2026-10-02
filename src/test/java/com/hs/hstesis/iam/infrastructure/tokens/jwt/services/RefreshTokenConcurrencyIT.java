package com.hs.hstesis.iam.infrastructure.tokens.jwt.services;

import com.hs.hstesis.iam.domain.model.aggregates.User;
import com.hs.hstesis.iam.domain.model.commands.CreateUserCommand;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.RefreshTokenRepository;
import com.hs.hstesis.iam.infrastructure.persistance.jpa.repositories.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RefreshTokenConcurrencyIT {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Test void simultaneousLogoutsAreIdempotentAndSignInCanReplaceTheToken() throws Exception {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").cleanDisabled(true).load().migrate();
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        factory.setPackagesToScan("com.hs.hstesis");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate",
                "hibernate.physical_naming_strategy", "com.hs.hstesis.shared.infrastructure.persistance.jpa.strategy.SnakeCasePhysicalNamingStrategy"));
        factory.afterPropertiesSet();
        try {
            var entityManagerFactory = factory.getObject();
            var transactions = new TransactionTemplate(new JpaTransactionManager(entityManagerFactory));
            transactions.setTimeout(20);
            var repositories = new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory));
            var users = repositories.getRepository(UserRepository.class);
            var tokens = repositories.getRepository(RefreshTokenRepository.class);
            var service = new RefreshTokenServiceImpl(tokens, users);
            Long userId = transactions.execute(status -> users.saveAndFlush(
                    new User(new CreateUserCommand("Synthetic User", "concurrent-fixture", "synthetic-hash"))).getId());

            // Every service invocation runs in its own committed transaction,
            // as @Transactional does in production. Threads share no EntityManager.
            for (int round = 0; round < 3; round++) {
                var token = transactions.execute(status -> service.createRefreshToken(userId));
                boolean present = transactions.execute(status -> tokens.findByToken(token.getToken()).isPresent());
                assertThat(present).isTrue();
                var ready = new CountDownLatch(4);
                var start = new CountDownLatch(1);
                try (var executor = Executors.newFixedThreadPool(4)) {
                    var attempts = new ArrayList<Future<?>>();
                    for (int i = 0; i < 4; i++) {
                        attempts.add(executor.submit(() -> {
                            ready.countDown();
                            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test start timed out");
                            transactions.executeWithoutResult(status -> service.deleteByUserId(userId));
                            return null;
                        }));
                    }
                    try {
                        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                    } finally {
                        start.countDown();
                    }
                    for (var attempt : attempts) attempt.get(30, TimeUnit.SECONDS);
                }
                boolean absent = transactions.execute(status -> tokens.findByToken(token.getToken()).isEmpty());
                long remaining = transactions.execute(status -> tokens.count());
                assertThat(absent).isTrue();
                assertThat(remaining).isZero();
                transactions.executeWithoutResult(status -> service.deleteByUserId(userId));
            }
            var first = transactions.execute(status -> service.createRefreshToken(userId));
            var replacement = transactions.execute(status -> service.createRefreshToken(userId));
            boolean originalAbsent = transactions.execute(status -> tokens.findByToken(first.getToken()).isEmpty());
            boolean replacementPresent = transactions.execute(status -> tokens.findByToken(replacement.getToken()).isPresent());
            long remaining = transactions.execute(status -> tokens.count());
            assertThat(originalAbsent).isTrue();
            assertThat(replacementPresent).isTrue();
            assertThat(remaining).isEqualTo(1);
        } finally {
            factory.destroy();
        }
    }
}
