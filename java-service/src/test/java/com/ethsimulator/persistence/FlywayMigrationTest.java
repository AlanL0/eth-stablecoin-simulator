package com.ethsimulator.persistence;

import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import com.ethsimulator.persistence.support.PostgresTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest extends AbstractPostgresIntegrationTest {

    @Test
    void migratesEmptyDatabaseThroughV1AndV2() {
        List<String> versions = PostgresTestSupport.appliedMigrationVersions(dataSource);

        assertThat(versions).containsExactly("1", "2");
        assertThat(tableExists("protocol_presets")).isTrue();
        assertThat(tableExists("ingestion_cursors")).isTrue();
        assertThat(tableExists("price_observations")).isTrue();
        assertThat(tableExists("rate_observations")).isTrue();
        assertThat(tableExists("source_health")).isTrue();
        assertThat(viewExists("price_observations_latest")).isTrue();
        assertThat(viewExists("rate_observations_history")).isTrue();
    }

    private boolean tableExists(String tableName) {
        return Boolean.TRUE.equals(jdbcClient.sql("""
                        select exists (
                          select 1
                          from information_schema.tables
                          where table_schema = 'public' and table_name = ?
                        )
                        """)
                .param(tableName)
                .query(Boolean.class)
                .single());
    }

    private boolean viewExists(String viewName) {
        return Boolean.TRUE.equals(jdbcClient.sql("""
                        select exists (
                          select 1
                          from information_schema.views
                          where table_schema = 'public' and table_name = ?
                        )
                        """)
                .param(viewName)
                .query(Boolean.class)
                .single());
    }
}