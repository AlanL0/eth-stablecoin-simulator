package com.ethsimulator.persistence;

import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SourceHealthRepositoryTest extends AbstractPostgresIntegrationTest {

    private SourceHealthRepository repository;

    @BeforeEach
    void setUpRepository() {
        repository = new SourceHealthRepository(jdbcClient);
    }

    @Test
    void upsertsSanitizedHealthSnapshot() {
        var health = new SourceHealth(
                "chainlink:eth-usd",
                Instant.parse("2026-06-18T13:05:00Z"),
                null,
                0,
                2L,
                "healthy",
                "within_sla",
                null
        );

        repository.upsert(health);

        var loaded = repository.find("chainlink:eth-usd");
        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().status()).isEqualTo("healthy");
        assertThat(loaded.orElseThrow().lagBlocks()).isEqualTo(2L);
        assertThat(loaded.orElseThrow().reason()).isEqualTo("within_sla");
    }
}