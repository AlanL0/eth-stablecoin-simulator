package com.ethsimulator.persistence;

import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateObservationRepositoryTest extends AbstractPostgresIntegrationTest {

    private RateObservationRepository repository;

    @BeforeEach
    void setUpRepository() {
        repository = new RateObservationRepository(jdbcClient);
    }

    @Test
    void insertsRateObservationWithAprConvention() {
        var observation = RateObservation.newObservation(
                "aave",
                "GHO",
                "borrow",
                FinancialMath.bd("0.048500000000000000"),
                "APR",
                "trailing_realized",
                "7d",
                "0xgho",
                1L,
                21_000_010L,
                "0xblock10",
                Instant.parse("2026-06-18T12:10:00Z"),
                Instant.parse("2026-06-18T12:09:55Z"),
                true,
                false
        );

        var id = repository.insert(observation);

        assertThat(id).isNotNull();
        assertThat(jdbcClient.sql("select annualized_value from rate_observations where id = ?")
                .param(id)
                .query(String.class)
                .single())
                .isEqualTo("0.048500000000000000");
    }

    @Test
    void rejectsDuplicateRateRows() {
        var observation = RateObservation.newObservation(
                "maker",
                "DAI",
                "stability_fee",
                FinancialMath.bd("0.050000000000000000"),
                "APR",
                "on_chain",
                null,
                "0xdai",
                1L,
                21_000_011L,
                "0xblock11",
                Instant.parse("2026-06-18T12:11:00Z"),
                null,
                true,
                false
        );

        repository.insert(observation);

        assertThatThrownBy(() -> repository.insert(observation))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsInvalidConvention() {
        var invalid = RateObservation.newObservation(
                "maker",
                "DAI",
                "stability_fee",
                FinancialMath.bd("0.050000000000000000"),
                "IRR",
                "on_chain",
                null,
                "0xdai",
                1L,
                21_000_012L,
                "0xblock12",
                Instant.parse("2026-06-18T12:12:00Z"),
                null,
                true,
                false
        );

        assertThatThrownBy(() -> repository.insert(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}