package com.ethsimulator.persistence;

import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceObservationRepositoryTest extends AbstractPostgresIntegrationTest {

    private PriceObservationRepository repository;

    @BeforeEach
    void setUpRepository() {
        repository = new PriceObservationRepository(jdbcClient);
    }

    @Test
    void insertsObservationWithNumericPrecision() {
        var observation = PriceObservation.newObservation(
                "ETH",
                "USD",
                FinancialMath.bd("3812.450000000000000001"),
                "chainlink",
                1L,
                21_000_000L,
                "0xabc123",
                "round-1",
                Instant.parse("2026-06-18T12:00:00Z"),
                Instant.parse("2026-06-18T11:59:50Z"),
                false,
                true,
                false
        );

        var id = repository.insert(observation);

        assertThat(id).isNotNull();
        assertThat(jdbcClient.sql("select value from price_observations where id = ?")
                .param(id)
                .query(String.class)
                .single())
                .isEqualTo("3812.450000000000000001");
    }

    @Test
    void rejectsDuplicateIdempotentRows() {
        var observation = PriceObservation.newObservation(
                "ETH",
                "USD",
                FinancialMath.bd("3800"),
                "chainlink",
                1L,
                21_000_001L,
                "0xabc124",
                "round-2",
                Instant.parse("2026-06-18T12:01:00Z"),
                null,
                false,
                true,
                false
        );

        repository.insert(observation);

        assertThatThrownBy(() -> repository.insert(observation))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsFinalizedObservationWithoutBlockHash() {
        var invalid = PriceObservation.newObservation(
                "ETH",
                "USD",
                FinancialMath.bd("3800"),
                "chainlink",
                1L,
                21_000_002L,
                " ",
                null,
                Instant.parse("2026-06-18T12:02:00Z"),
                null,
                false,
                true,
                false
        );

        assertThatThrownBy(() -> repository.insert(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}