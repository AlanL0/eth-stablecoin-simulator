package com.ethsimulator.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

import java.util.Optional;

@Repository
@ConditionalOnBean(DataSource.class)
public class SourceHealthRepository {

    private final JdbcClient jdbcClient;

    public SourceHealthRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void upsert(SourceHealth health) {
        jdbcClient.sql("""
                insert into source_health (
                  source_key, last_success_at, last_failure_at, consecutive_failures,
                  lag_blocks, status, reason
                ) values (?, ?, ?, ?, ?, ?, ?)
                on conflict (source_key) do update set
                  last_success_at = excluded.last_success_at,
                  last_failure_at = excluded.last_failure_at,
                  consecutive_failures = excluded.consecutive_failures,
                  lag_blocks = excluded.lag_blocks,
                  status = excluded.status,
                  reason = excluded.reason,
                  updated_at = now()
                """)
                .param(health.sourceKey())
                .param(JdbcBindings.toTimestamp(health.lastSuccessAt()))
                .param(JdbcBindings.toTimestamp(health.lastFailureAt()))
                .param(health.consecutiveFailures())
                .param(health.lagBlocks())
                .param(health.status())
                .param(health.reason())
                .update();
    }

    public Optional<SourceHealth> find(String sourceKey) {
        return jdbcClient.sql("""
                select source_key, last_success_at, last_failure_at, consecutive_failures,
                       lag_blocks, status, reason, updated_at
                from source_health
                where source_key = ?
                """)
                .param(sourceKey)
                .query(PersistenceRowMappers.SOURCE_HEALTH)
                .optional();
    }
}