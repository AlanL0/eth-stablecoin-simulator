package com.ethsimulator.persistence;

import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RateReadModelTest extends AbstractPostgresIntegrationTest {

    private RateObservationRepository writeRepository;
    private RateReadModelRepository readRepository;

    @BeforeEach
    void setUpRepositories() {
        writeRepository = new RateObservationRepository(jdbcClient);
        readRepository = new RateReadModelRepository(jdbcClient);
    }

    @Test
    void latestViewSelectsHighestFinalizedBlockAndExcludesRevertedRows() {
        insert(21_000_120L, "0.040000000000000000", false);
        insert(21_000_125L, "0.045000000000000000", false);
        insert(21_000_130L, "0.060000000000000000", true);

        var latest = readRepository.findLatest("aave", "GHO", "borrow");

        assertThat(latest).isPresent();
        assertThat(latest.orElseThrow().blockNumber()).isEqualTo(21_000_125L);
        assertThat(latest.orElseThrow().annualizedValue())
                .isEqualByComparingTo(FinancialMath.bd("0.045000000000000000"));
    }

    @Test
    void historyViewReturnsOnlyFinalizedNonRevertedRates() {
        insert(21_000_140L, "0.040000000000000000", false);
        insert(21_000_141L, "0.050000000000000000", true);
        insert(21_000_142L, "0.055000000000000000", false);

        var history = readRepository.findHistory("aave", "GHO", "borrow", 10);

        assertThat(history).hasSize(2);
        assertThat(history.getFirst().blockNumber()).isEqualTo(21_000_142L);
        assertThat(history.get(1).blockNumber()).isEqualTo(21_000_140L);
    }

    private void insert(long blockNumber, String annualizedValue, boolean reverted) {
        writeRepository.insert(RateObservation.newObservation(
                "aave",
                "GHO",
                "borrow",
                FinancialMath.bd(annualizedValue),
                "APR",
                "trailing_realized",
                "7d",
                "0xgho",
                1L,
                blockNumber,
                "0xblock" + blockNumber,
                Instant.parse("2026-06-18T12:00:00Z").plusSeconds(blockNumber),
                null,
                true,
                reverted
        ));
    }
}