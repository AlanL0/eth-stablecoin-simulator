package com.ethsimulator.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnBean(DataSource.class)
public class RateReadModelRepository {

    private final JdbcClient jdbcClient;

    public RateReadModelRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<RateObservation> findLatest(String protocol, String product, String side) {
        return jdbcClient.sql("""
                select id, protocol, product, side, annualized_value, convention, methodology,
                       lookback_window, contract_address, chain_id, block_number, block_hash,
                       observed_at, source_timestamp, created_at
                from rate_observations_latest
                where protocol = ? and product = ? and side = ?
                """)
                .param(protocol)
                .param(product)
                .param(side)
                .query(PersistenceRowMappers.RATE_OBSERVATION)
                .optional();
    }

    public List<RateObservation> findLatestByProducts(List<String> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", products.stream().map(product -> "?").toList());
        var statement = jdbcClient.sql("""
                select id, protocol, product, side, annualized_value, convention, methodology,
                       lookback_window, contract_address, chain_id, block_number, block_hash,
                       observed_at, source_timestamp, created_at
                from rate_observations_latest
                where upper(product) in (""" + placeholders + ")");
        for (String product : products) {
            statement = statement.param(product.toUpperCase());
        }
        return statement.query(PersistenceRowMappers.RATE_OBSERVATION).list();
    }

    public List<RateObservation> findHistory(String protocol, String product, String side, int limit) {
        return jdbcClient.sql("""
                select id, protocol, product, side, annualized_value, convention, methodology,
                       lookback_window, contract_address, chain_id, block_number, block_hash,
                       observed_at, source_timestamp, created_at
                from rate_observations_history
                where protocol = ? and product = ? and side = ?
                limit ?
                """)
                .param(protocol)
                .param(product)
                .param(side)
                .param(limit)
                .query(PersistenceRowMappers.RATE_OBSERVATION)
                .list();
    }
}