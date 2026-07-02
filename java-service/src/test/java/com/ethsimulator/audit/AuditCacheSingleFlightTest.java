package com.ethsimulator.audit;

import com.ethsimulator.blockchain.TransferEventFetcher;
import com.ethsimulator.blockchain.TransferEventRecord;
import com.ethsimulator.config.EthSimulatorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditCacheSingleFlightTest {

    private static final String ADDRESS = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045";
    private static final String NORMALIZED_ADDRESS = ADDRESS.toLowerCase(Locale.ROOT);
    private static final Instant EVENT_TIME = Instant.parse("2026-01-10T00:00:00Z");

    @Mock
    private TransferEventFetcher transferEventFetcher;

    private AuditCache auditCache;

    @BeforeEach
    void setUp() {
        EthSimulatorProperties properties = new EthSimulatorProperties();
        properties.setAuditCacheMaxAddresses(100);
        properties.setAuditCacheTtlSeconds(900);
        auditCache = new AuditCache(transferEventFetcher, Clock.systemUTC(), properties);
    }

    @Test
    void concurrentMissesTriggerSingleFetch() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger fetchCount = new AtomicInteger();

        when(transferEventFetcher.fetchTransferEvents(eq(NORMALIZED_ADDRESS))).thenAnswer(invocation -> {
            fetchCount.incrementAndGet();
            started.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return List.of(event("0xabc", 0));
        });

        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> auditCache.eventsFor(ADDRESS));
            }
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(fetchCount.get()).isEqualTo(1);
        assertThat(auditCache.eventsFor(ADDRESS)).hasSize(1);
    }

    @Test
    void completedFailureDoesNotBlockRetry() {
        AtomicInteger attempts = new AtomicInteger();
        when(transferEventFetcher.fetchTransferEvents(eq(NORMALIZED_ADDRESS))).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) {
                throw new RuntimeException("rpc down");
            }
            return List.of(event("0xretry", 2));
        });

        assertThatThrownBy(() -> auditCache.eventsFor(ADDRESS))
                .isInstanceOf(RuntimeException.class);

        assertThat(auditCache.eventsFor(ADDRESS)).hasSize(1);
        assertThat(auditCache.eventsFor(ADDRESS)).hasSize(1);
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void failedLoadDoesNotPoisonCacheAndRetries() {
        AtomicInteger attempts = new AtomicInteger();
        when(transferEventFetcher.fetchTransferEvents(eq(NORMALIZED_ADDRESS))).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) {
                throw new RuntimeException("rpc down");
            }
            return List.of(event("0xdef", 1));
        });

        assertThatThrownBy(() -> auditCache.eventsFor(ADDRESS))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("rpc down");

        assertThat(auditCache.eventsFor(ADDRESS)).hasSize(1);
        assertThat(attempts.get()).isEqualTo(2);
    }

    private TransferEventRecord event(String txHash, int logIndex) {
        return new TransferEventRecord(
                "USDC",
                txHash,
                logIndex,
                ADDRESS,
                "0x1111111111111111111111111111111111111111",
                new BigDecimal("1"),
                100L,
                EVENT_TIME
        );
    }
}