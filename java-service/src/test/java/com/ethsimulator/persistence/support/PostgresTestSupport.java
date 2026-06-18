package com.ethsimulator.persistence.support;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.List;

public final class PostgresTestSupport {

    public static final String POSTGRES_IMAGE = "postgres:17";

    private PostgresTestSupport() {
    }

    public static PostgreSQLContainer newContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }

    public static DataSource createDataSource(PostgreSQLContainer container) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(container.getJdbcUrl());
        dataSource.setUsername(container.getUsername());
        dataSource.setPassword(container.getPassword());
        dataSource.setMaximumPoolSize(4);
        return dataSource;
    }

    public static JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    public static Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .cleanDisabled(false)
                .load();
    }

    public static void migrateFresh(DataSource dataSource) {
        Flyway flyway = flyway(dataSource);
        flyway.clean();
        flyway.migrate();
    }

    public static void applyLegacyV1Schema(DataSource dataSource) {
        Flyway fresh = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        fresh.clean();
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("1")
                .load()
                .migrate();
        jdbcClient(dataSource).sql("drop table flyway_schema_history").update();
    }

    public static List<String> appliedMigrationVersions(DataSource dataSource) {
        return jdbcClient(dataSource)
                .sql("""
                        select version
                        from flyway_schema_history
                        where success = true
                        order by installed_rank
                        """)
                .query(String.class)
                .list();
    }
}