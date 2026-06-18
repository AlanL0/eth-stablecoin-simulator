package com.ethsimulator.persistence;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

final class PersistenceRowMappers {

    private PersistenceRowMappers() {
    }

    static final RowMapper<IngestionCursor> INGESTION_CURSOR = IngestionCursorRowMapper::mapRow;

    static final RowMapper<PriceObservation> PRICE_OBSERVATION = PriceObservationRowMapper::mapRow;

    static final RowMapper<RateObservation> RATE_OBSERVATION = RateObservationRowMapper::mapRow;

    static final RowMapper<SourceHealth> SOURCE_HEALTH = SourceHealthRowMapper::mapRow;

    private static final class IngestionCursorRowMapper {
        private IngestionCursorRowMapper() {
        }

        static IngestionCursor mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new IngestionCursor(
                    rs.getString("source_key"),
                    rs.getLong("chain_id"),
                    rs.getLong("next_block"),
                    (Long) rs.getObject("last_finalized_block"),
                    rs.getString("last_finalized_block_hash"),
                    rs.getString("lease_owner"),
                    readInstant(rs, "lease_expires_at"),
                    readInstant(rs, "created_at"),
                    readInstant(rs, "updated_at")
            );
        }
    }

    private static final class PriceObservationRowMapper {
        private PriceObservationRowMapper() {
        }

        static PriceObservation mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PriceObservation(
                    rs.getObject("id", UUID.class),
                    rs.getString("base_asset"),
                    rs.getString("quote_asset"),
                    rs.getBigDecimal("value"),
                    rs.getString("source"),
                    rs.getLong("chain_id"),
                    rs.getLong("block_number"),
                    rs.getString("block_hash"),
                    rs.getString("round_id"),
                    readInstant(rs, "observed_at"),
                    readInstant(rs, "source_timestamp"),
                    rs.getBoolean("is_stale"),
                    hasColumn(rs, "is_finalized") && rs.getBoolean("is_finalized"),
                    hasColumn(rs, "is_reverted") && rs.getBoolean("is_reverted"),
                    readInstant(rs, "created_at")
            );
        }
    }

    private static final class RateObservationRowMapper {
        private RateObservationRowMapper() {
        }

        static RateObservation mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RateObservation(
                    rs.getObject("id", UUID.class),
                    rs.getString("protocol"),
                    rs.getString("product"),
                    rs.getString("side"),
                    rs.getBigDecimal("annualized_value"),
                    rs.getString("convention"),
                    rs.getString("methodology"),
                    rs.getString("lookback_window"),
                    rs.getString("contract_address"),
                    rs.getLong("chain_id"),
                    rs.getLong("block_number"),
                    rs.getString("block_hash"),
                    readInstant(rs, "observed_at"),
                    readInstant(rs, "source_timestamp"),
                    hasColumn(rs, "is_finalized") && rs.getBoolean("is_finalized"),
                    hasColumn(rs, "is_reverted") && rs.getBoolean("is_reverted"),
                    readInstant(rs, "created_at")
            );
        }
    }

    private static final class SourceHealthRowMapper {
        private SourceHealthRowMapper() {
        }

        static SourceHealth mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SourceHealth(
                    rs.getString("source_key"),
                    readInstant(rs, "last_success_at"),
                    readInstant(rs, "last_failure_at"),
                    rs.getInt("consecutive_failures"),
                    (Long) rs.getObject("lag_blocks"),
                    rs.getString("status"),
                    rs.getString("reason"),
                    readInstant(rs, "updated_at")
            );
        }
    }

    private static Instant readInstant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static boolean hasColumn(ResultSet rs, String column) throws SQLException {
        var metadata = rs.getMetaData();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            if (column.equalsIgnoreCase(metadata.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }
}