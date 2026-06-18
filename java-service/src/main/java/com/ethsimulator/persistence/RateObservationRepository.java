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
}