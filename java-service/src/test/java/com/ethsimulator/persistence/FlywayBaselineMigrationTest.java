package com.ethsimulator.persistence;

import com.ethsimulator.persistence.support.PostgresTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayBaselineMigrationTest {

    private static PostgreSQLContainer POSTGRES;

    @BeforeAll
    static void startPostgres() {
        POSTGRES = PostgresTestSupport.newContainer();
        POSTGRES.start();
    }

    @AfterAll
    static void stopPostgres() {
        if (POSTGRES != null) {
            POSTGRES.stop();
        }
    }

    @Test
    void baselinesExistingSchemaAndAppliesOnlyCqrsMigrations() {
        DataSource dataSource = PostgresTestSupport.createDataSource(POSTGRES);
        PostgresTestSupport.applyLegacyV1Schema(dataSource);

        Flyway flyway = PostgresTestSupport.flyway(dataSource);
        flyway.migrate();

        List<String> versions = PostgresTestSupport.appliedMigrationVersions(dataSource);
        assertThat(versions).containsExactly("1", "2");

        JdbcClient jdbc = PostgresTestSupport.jdbcClient(dataSource);
        assertThat(jdbc.sql("select count(*) from protocol_presets").query(Integer.class).single())
                .isEqualTo(4);
        assertThat(jdbc.sql("select to_regclass('public.ingestion_cursors')").query(String.class).single())
                .isEqualTo("ingestion_cursors");
    }
}