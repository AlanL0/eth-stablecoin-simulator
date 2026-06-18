package com.ethsimulator.persistence;

import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionAdvisoryLockTest extends AbstractPostgresIntegrationTest {

    private static final String SOURCE_KEY = "chainlink:eth-usd";

    private IngestionCursorRepository cursorRepository;

    @BeforeEach
    void setUpRepositories() {
        cursorRepository = new IngestionCursorRepository(jdbcClient);
        cursorRepository.upsert(new IngestionCursor(
                SOURCE_KEY,
                1L,
                21_000_200L,
                21_000_199L,
                "0xfinal",
                null,
                null,
                null,
                null
        ));
    }

    @Test
    void advisoryLockAllowsSingleWinnerAndSafeRelease() {
        try (var leaderA = openConnection();
             var leaderB = openConnection()) {
            var lockA = new IngestionLeaderLock(JdbcClient.create(leaderA));
            var lockB = new IngestionLeaderLock(JdbcClient.create(leaderB));

            assertThat(lockA.tryAcquire(SOURCE_KEY, 1L, "worker-a", Duration.ofMinutes(5))).isTrue();
            assertThat(lockB.tryAcquire(SOURCE_KEY, 1L, "worker-b", Duration.ofMinutes(5))).isFalse();

            lockA.release(SOURCE_KEY, 1L);

            assertThat(lockB.tryAcquire(SOURCE_KEY, 1L, "worker-b", Duration.ofMinutes(5))).isTrue();
        }
    }

    @Test
    void expiredLeaseAllowsNewLeaderAfterPriorOwnerDisappears() {
        cursorRepository.upsert(new IngestionCursor(
                SOURCE_KEY,
                1L,
                21_000_200L,
                21_000_199L,
                "0xfinal",
                "stale-worker",
                Instant.parse("2020-01-01T00:00:00Z"),
                null,
                null
        ));

        try (var leader = openConnection()) {
            var lock = new IngestionLeaderLock(JdbcClient.create(leader));

            assertThat(lock.tryAcquire(SOURCE_KEY, 1L, "fresh-worker", Duration.ofMinutes(5))).isTrue();

            var lease = lock.findLease(SOURCE_KEY, 1L);
            assertThat(lease).isPresent();
            assertThat(lease.orElseThrow().leaseOwner()).isEqualTo("fresh-worker");
            assertThat(lease.orElseThrow().leaseExpiresAt()).isAfter(Instant.now());
        }
    }

    private HikariDataSource openConnection() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(POSTGRES.getJdbcUrl());
        ds.setUsername(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        ds.setMaximumPoolSize(1);
        return ds;
    }
}