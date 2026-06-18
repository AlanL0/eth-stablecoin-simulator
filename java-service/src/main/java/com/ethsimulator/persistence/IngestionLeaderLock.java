package com.ethsimulator.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * PostgreSQL advisory lock plus durable lease metadata on {@code ingestion_cursors}.
 * Ensures a single ingestion leader per source/chain with safe expiry after crashes.
 */
@Component
@ConditionalOnBean(DataSource.class)
public class IngestionLeaderLock {

    /**
     * Stable advisory lock key for ETH mainnet ingestion (T21 will namespace per workload).
     */
    public static final long INGESTION_ADVISORY_LOCK_KEY = 1_928_374_651L;

    private final JdbcClient jdbcClient;

    public IngestionLeaderLock(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean tryAcquire(String sourceKey, long chainId, String owner, Duration leaseDuration) {
        Boolean acquired = jdbcClient.sql("select pg_try_advisory_lock(?)")
                .param(INGESTION_ADVISORY_LOCK_KEY)
                .query(Boolean.class)
                .single();
        if (!Boolean.TRUE.equals(acquired)) {
            return false;
        }

        Instant expiresAt = Instant.now().plus(leaseDuration);
        int updated = jdbcClient.sql("""
                update ingestion_cursors
                set lease_owner = ?,
                    lease_expires_at = ?,
                    updated_at = now()
                where source_key = ?
                  and chain_id = ?
                  and (
                    lease_expires_at is null
                    or lease_expires_at < now()
                    or lease_owner = ?
                  )
                """)
                .param(owner)
                .param(JdbcBindings.toTimestamp(expiresAt))
                .param(sourceKey)
                .param(chainId)
                .param(owner)
                .update();

        if (updated == 0) {
            release(sourceKey, chainId);
            return false;
        }
        return true;
    }

    public void release(String sourceKey, long chainId) {
        jdbcClient.sql("select pg_advisory_unlock(?)")
                .param(INGESTION_ADVISORY_LOCK_KEY)
                .query(Boolean.class)
                .single();
        jdbcClient.sql("""
                update ingestion_cursors
                set lease_owner = null,
                    lease_expires_at = null,
                    updated_at = now()
                where source_key = ? and chain_id = ?
                """)
                .param(sourceKey)
                .param(chainId)
                .update();
    }

    public Optional<IngestionCursor> findLease(String sourceKey, long chainId) {
        return jdbcClient.sql("""
                select source_key, chain_id, next_block, last_finalized_block,
                       last_finalized_block_hash, lease_owner, lease_expires_at,
                       created_at, updated_at
                from ingestion_cursors
                where source_key = ? and chain_id = ?
                """)
                .param(sourceKey)
                .param(chainId)
                .query(PersistenceRowMappers.INGESTION_CURSOR)
                .optional();
    }

    public boolean isLeaseExpired(String sourceKey, long chainId, Instant now) {
        return jdbcClient.sql("""
                select lease_expires_at
                from ingestion_cursors
                where source_key = ? and chain_id = ?
                """)
                .param(sourceKey)
                .param(chainId)
                .query(Timestamp.class)
                .optional()
                .map(expiresAt -> expiresAt == null || expiresAt.toInstant().isBefore(now))
                .orElse(true);
    }
}