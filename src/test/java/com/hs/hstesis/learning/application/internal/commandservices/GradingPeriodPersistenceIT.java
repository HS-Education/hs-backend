package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.GradingPeriodDurationTooShortException;
import com.hs.hstesis.learning.domain.model.aggregates.GradingPeriod;
import com.hs.hstesis.learning.domain.model.commands.UpdateGradingPeriodCommand;
import com.hs.hstesis.learning.domain.model.entities.AcademicYear;
import com.hs.hstesis.learning.domain.model.valueobjects.Bimester;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.GradingPeriodRepository;
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

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
class GradingPeriodPersistenceIT {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Test void twoWeekDatesSurviveReloadAndARejectedUpdateLeavesThemUnchanged() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").cleanDisabled(true).load().migrate();
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        factory.setPackagesToScan("com.hs.hstesis");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate",
                "hibernate.physical_naming_strategy", "com.hs.hstesis.shared.infrastructure.persistance.jpa.strategy.SnakeCasePhysicalNamingStrategy"));
        factory.afterPropertiesSet();
        var today = LocalDate.of(2030, 3, 1);
        var start = LocalDate.of(2030, 4, 1);
        try (var dates = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            dates.when(LocalDate::now).thenReturn(today);
            var emf = factory.getObject();
            var transactions = new TransactionTemplate(new JpaTransactionManager(emf));
            var entities = SharedEntityManagerCreator.createSharedEntityManager(emf);
            var periods = new JpaRepositoryFactory(entities).getRepository(GradingPeriodRepository.class);
            var service = new GradingPeriodCommandServiceImpl(periods);
            Long yearId = transactions.execute(status -> {
                var year = new AcademicYear(today.getYear());
                entities.persist(year);
                entities.flush();
                return year.getId();
            });
            Long periodId = transactions.execute(status -> periods.saveAndFlush(new GradingPeriod(
                    Bimester.FIRST, entities.find(AcademicYear.class, yearId))).getId());

            transactions.executeWithoutResult(status -> service.handle(new UpdateGradingPeriodCommand(
                    yearId, periodId, start, start.plusDays(14))));
            var persisted = transactions.execute(status -> periods.findById(periodId).orElseThrow());
            assertThat(persisted.getStartDate()).isEqualTo(start);
            assertThat(persisted.getEndDate()).isEqualTo(start.plusDays(14));

            assertThatThrownBy(() -> transactions.executeWithoutResult(status -> service.handle(
                    new UpdateGradingPeriodCommand(yearId, periodId, start, start.plusDays(13)))))
                    .isInstanceOf(GradingPeriodDurationTooShortException.class);
            var unchanged = transactions.execute(status -> periods.findById(periodId).orElseThrow());
            assertThat(unchanged.getStartDate()).isEqualTo(start);
            assertThat(unchanged.getEndDate()).isEqualTo(start.plusDays(14));
        } finally {
            factory.destroy();
        }
    }
}
