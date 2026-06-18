package com.ethsimulator.persistence;

import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionCursorRepositoryTest extends AbstractPostgresIntegrationTest {

    private IngestionCursorRepository repository;

    @BeforeEach
    void setUpRepository() {
        repository = new IngestionCursorRepository(jdbcClient);
    }

    @Test
    void upsertsCursorWithFinalizedBlockMetadata() {
        var cursor = new IngestionCursor(
                "chainlink:eth-usd",
                1L,
                21_000_100L,
                21_000_099L,
                "0xfinalized",
                "worker-a",
                Instant.parse("2026-06-18T13:00:00Z"),
                null,
                null
        );

        repository.upsert(cursor);

        var loaded = repository.find("chainlink:eth-usd", 1L);
        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().nextBlock()).isEqualTo(21_000_100L);
        assertThat(loaded.orElseThrow().lastFinalizedBlockHash()).isEqualTo("0xfinalized");
        assertThat(loaded.orElseThrow().leaseOwner()).isEqualTo("worker-a");
    }
}