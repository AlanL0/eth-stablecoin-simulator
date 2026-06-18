package com.ethsimulator.persistence.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

public abstract class AbstractPostgresIntegrationTest {

    protected static PostgreSQLContainer POSTGRES;

    protected DataSource dataSource;
    protected JdbcClient jdbcClient;

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

    @BeforeEach
    void setUpDatabase() {
        dataSource = PostgresTestSupport.createDataSource(POSTGRES);
        PostgresTestSupport.migrateFresh(dataSource);
        jdbcClient = PostgresTestSupport.jdbcClient(dataSource);
    }
}