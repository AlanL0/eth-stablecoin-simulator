package com.ethsimulator.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

import java.util.UUID;

@Repository
@ConditionalOnBean(DataSource.class)
public class PriceObservationRepository {

    private final JdbcClient jdbcClient;

    public PriceObservationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public UUID insert(PriceObservation observation) {
        return jdbcClient.sql("""
                insert into price_observations (
                  base_asset, quote_asset, value, source, chain_id, block_number,
                  block_hash, round_id, observed_at, source_timestamp,
                  is_stale, is_finalized, is_reverted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning id
                """)
                .param(observation.baseAsset())
                .param(observation.quoteAsset())
                .param(observation.value())
                .param(observation.source())
                .param(observation.chainId())
                .param(observation.blockNumber())
                .param(observation.blockHash())
                .param(observation.roundId())
                .param(JdbcBindings.toTimestamp(observation.observedAt()))
                .param(JdbcBindings.toTimestamp(observation.sourceTimestamp()))
                .param(observation.stale())
                .param(observation.finalized())
                .param(observation.reverted())
                .query(UUID.class)
                .single();
    }

    public void insertIdempotent(PriceObservation observation) {
        jdbcClient.sql("""
                insert into price_observations (
                  base_asset, quote_asset, value, source, chain_id, block_number,
                  block_hash, round_id, observed_at, source_timestamp,
                  is_stale, is_finalized, is_reverted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (source, chain_id, block_number, base_asset, quote_asset, coalesce(round_id, ''))
                do update set
                  block_hash = excluded.block_hash,
                  value = excluded.value,
                  observed_at = excluded.observed_at,
                  source_timestamp = excluded.source_timestamp,
                  is_stale = excluded.is_stale,
                  is_reverted = false
                where price_observations.is_reverted = true
                """)
                .param(observation.baseAsset())
                .param(observation.quoteAsset())
                .param(observation.value())
                .param(observation.source())
                .param(observation.chainId())
                .param(observation.blockNumber())
                .param(observation.blockHash())
                .param(observation.roundId())
                .param(JdbcBindings.toTimestamp(observation.observedAt()))
                .param(JdbcBindings.toTimestamp(observation.sourceTimestamp()))
                .param(observation.stale())
                .param(observation.finalized())
                .param(observation.reverted())
                .update();
    }

    public int markReverted(String source, long chainId, long fromBlock, long throughBlock) {
        return jdbcClient.sql("""
                update price_observations
                set is_reverted = true
                where source = ?
                  and chain_id = ?
                  and block_number between ? and ?
                  and is_reverted = false
                """)
                .param(source)
                .param(chainId)
                .param(fromBlock)
                .param(throughBlock)
                .update();
    }
}