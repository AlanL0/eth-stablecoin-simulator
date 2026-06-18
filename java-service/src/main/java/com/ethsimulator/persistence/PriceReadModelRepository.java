package com.ethsimulator.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnBean(DataSource.class)
public class PriceReadModelRepository {

    private final JdbcClient jdbcClient;

    public PriceReadModelRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<PriceObservation> findLatest(String baseAsset, String quoteAsset, String source) {
        return jdbcClient.sql("""
                select id, base_asset, quote_asset, value, source, chain_id, block_number,
                       block_hash, round_id, observed_at, source_timestamp, is_stale, created_at
                from price_observations_latest
                where base_asset = ? and quote_asset = ? and source = ?
                """)
                .param(baseAsset)
                .param(quoteAsset)
                .param(source)
                .query(PersistenceRowMappers.PRICE_OBSERVATION)
                .optional();
    }

    public List<PriceObservation> findHistory(String baseAsset, String quoteAsset, int limit) {
        return jdbcClient.sql("""
                select id, base_asset, quote_asset, value, source, chain_id, block_number,
                       block_hash, round_id, observed_at, source_timestamp, is_stale, created_at
                from price_observations_history
                where base_asset = ? and quote_asset = ?
                limit ?
                """)
                .param(baseAsset)
                .param(quoteAsset)
                .param(limit)
                .query(PersistenceRowMappers.PRICE_OBSERVATION)
                .list();
    }
}