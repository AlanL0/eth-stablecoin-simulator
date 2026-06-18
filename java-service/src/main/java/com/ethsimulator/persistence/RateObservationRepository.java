package com.ethsimulator.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

import java.util.UUID;

@Repository
@ConditionalOnBean(DataSource.class)
public class RateObservationRepository {

    private final JdbcClient jdbcClient;

    public RateObservationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public UUID insert(RateObservation observation) {
        return jdbcClient.sql("""
                insert into rate_observations (
                  protocol, product, side, annualized_value, convention, methodology,
                  lookback_window, contract_address, chain_id, block_number, block_hash,
                  observed_at, source_timestamp, is_finalized, is_reverted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning id
                """)
                .param(observation.protocol())
                .param(observation.product())
                .param(observation.side())
                .param(observation.annualizedValue())
                .param(observation.convention())
                .param(observation.methodology())
                .param(observation.lookbackWindow())
                .param(observation.contractAddress())
                .param(observation.chainId())
                .param(observation.blockNumber())
                .param(observation.blockHash())
                .param(JdbcBindings.toTimestamp(observation.observedAt()))
                .param(JdbcBindings.toTimestamp(observation.sourceTimestamp()))
                .param(observation.finalized())
                .param(observation.reverted())
                .query(UUID.class)
                .single();
    }

    public void insertIdempotent(RateObservation observation) {
        jdbcClient.sql("""
                insert into rate_observations (
                  protocol, product, side, annualized_value, convention, methodology,
                  lookback_window, contract_address, chain_id, block_number, block_hash,
                  observed_at, source_timestamp, is_finalized, is_reverted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (protocol, product, side, chain_id, block_number, coalesce(contract_address, ''))
                do update set
                  block_hash = excluded.block_hash,
                  annualized_value = excluded.annualized_value,
                  observed_at = excluded.observed_at,
                  source_timestamp = excluded.source_timestamp,
                  is_reverted = false
                where rate_observations.is_reverted = true
                """)
                .param(observation.protocol())
                .param(observation.product())
                .param(observation.side())
                .param(observation.annualizedValue())
                .param(observation.convention())
                .param(observation.methodology())
                .param(observation.lookbackWindow())
                .param(observation.contractAddress())
                .param(observation.chainId())
                .param(observation.blockNumber())
                .param(observation.blockHash())
                .param(JdbcBindings.toTimestamp(observation.observedAt()))
                .param(JdbcBindings.toTimestamp(observation.sourceTimestamp()))
                .param(observation.finalized())
                .param(observation.reverted())
                .update();
    }

    public int markReverted(String protocol, long chainId, long fromBlock, long throughBlock) {
        return jdbcClient.sql("""
                update rate_observations
                set is_reverted = true
                where protocol = ?
                  and chain_id = ?
                  and block_number between ? and ?
                  and is_reverted = false
                """)
                .param(protocol)
                .param(chainId)
                .param(fromBlock)
                .param(throughBlock)
                .update();
    }
}