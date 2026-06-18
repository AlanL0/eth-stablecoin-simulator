package com.ethsimulator.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

import java.util.Optional;

@Repository
@ConditionalOnBean(DataSource.class)
public class IngestionCursorRepository {

    private final JdbcClient jdbcClient;

    public IngestionCursorRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void upsert(IngestionCursor cursor) {
        jdbcClient.sql("""
                insert into ingestion_cursors (
                  source_key, chain_id, next_block, last_finalized_block,
                  last_finalized_block_hash, lease_owner, lease_expires_at
                ) values (?, ?, ?, ?, ?, ?, ?)
                on conflict (source_key, chain_id) do update set
                  next_block = excluded.next_block,
                  last_finalized_block = excluded.last_finalized_block,
                  last_finalized_block_hash = excluded.last_finalized_block_hash,
                  lease_owner = excluded.lease_owner,
                  lease_expires_at = excluded.lease_expires_at,
                  updated_at = now()
                """)
                .param(cursor.sourceKey())
                .param(cursor.chainId())
                .param(cursor.nextBlock())
                .param(cursor.lastFinalizedBlock())
                .param(cursor.lastFinalizedBlockHash())
                .param(cursor.leaseOwner())
                .param(JdbcBindings.toTimestamp(cursor.leaseExpiresAt()))
                .update();
    }

    public Optional<IngestionCursor> find(String sourceKey, long chainId) {
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
}