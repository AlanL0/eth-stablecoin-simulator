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
}