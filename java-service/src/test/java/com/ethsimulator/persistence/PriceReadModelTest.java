package com.ethsimulator.persistence;

import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PriceReadModelTest extends AbstractPostgresIntegrationTest {

    private PriceObservationRepository writeRepository;
    private PriceReadModelRepository readRepository;

    @BeforeEach
    void setUpRepositories() {
        writeRepository = new PriceObservationRepository(jdbcClient);
        readRepository = new PriceReadModelRepository(jdbcClient);
    }

    @Test
    void latestViewSelectsHighestFinalizedBlockAndExcludesRevertedRows() {
        insert("chainlink", 21_000_020L, "3800", false, true, false);
        insert("chainlink", 21_000_025L, "3900", false, true, false);
        insert("chainlink", 21_000_030L, "4100", false, true, true);
        insert("chainlink", 21_000_035L, "4200", false, false, false);

        var latest = readRepository.findLatest("ETH", "USD", "chainlink");

        assertThat(latest).isPresent();
        assertThat(latest.orElseThrow().blockNumber()).isEqualTo(21_000_025L);
        assertThat(latest.orElseThrow().value()).isEqualByComparingTo(FinancialMath.bd("3900"));
    }

    @Test
    void historyViewReturnsOnlyFinalizedNonRevertedObservations() {
        insert("chainlink", 21_000_040L, "3800", false, true, false);
        insert("chainlink", 21_000_041L, "3850", false, true, true);
        insert("chainlink", 21_000_042L, "3900", false, true, false);

        var history = readRepository.findHistory("ETH", "USD", 10);

        assertThat(history).hasSize(2);
        assertThat(history.getFirst().blockNumber()).isEqualTo(21_000_042L);
        assertThat(history.get(1).blockNumber()).isEqualTo(21_000_040L);
    }

    private void insert(
            String source,
            long blockNumber,
            String value,
            boolean stale,
            boolean finalized,
            boolean reverted
    ) {
        writeRepository.insert(PriceObservation.newObservation(
                "ETH",
                "USD",
                FinancialMath.bd(value),
                source,
                1L,
                blockNumber,
                "0xblock" + blockNumber,
                "round-" + blockNumber,
                Instant.parse("2026-06-18T12:00:00Z").plusSeconds(blockNumber),
                null,
                stale,
                finalized,
                reverted
        ));
    }
}