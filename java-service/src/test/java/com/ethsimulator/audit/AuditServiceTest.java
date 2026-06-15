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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private static final String ADDRESS = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045";
    private static final Instant OLDER = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant MIDDLE = Instant.parse("2026-01-10T00:00:00Z");
    private static final Instant NEWER = Instant.parse("2026-01-20T00:00:00Z");

    @Mock
    private TransferEventFetcher transferEventFetcher;

    private AuditCache auditCache;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        EthSimulatorProperties properties = new EthSimulatorProperties();
        properties.setAuditCacheMaxAddresses(100);
        properties.setAuditCacheTtlSeconds(900);
        auditCache = new AuditCache(transferEventFetcher, Clock.systemUTC(), properties);
        auditService = new AuditService(auditCache);
        when(transferEventFetcher.source()).thenReturn("chain");
    }

    @Test
    void cachesEventsAndDedupesByTxHashAndLogIndex() {
        when(transferEventFetcher.fetchTransferEvents(ADDRESS)).thenReturn(List.of(
                event("USDC", "0xaaa", 0, OLDER),
                event("USDC", "0xaaa", 0, OLDER),
                event("DAI", "0xbbb", 1, NEWER)
        ));

        AuditResponse first = auditService.audit(ADDRESS, null, null, null, false);
        AuditResponse second = auditService.audit(ADDRESS, null, null, null, false);

        assertThat(first.events()).hasSize(2);
        assertThat(second.events()).hasSize(2);
        verify(transferEventFetcher, times(1)).fetchTransferEvents(ADDRESS);
    }

    @Test
    void filtersByDateRange() {
        when(transferEventFetcher.fetchTransferEvents(ADDRESS)).thenReturn(List.of(
                event("USDC", "0xaaa", 0, OLDER),
                event("USDC", "0xbbb", 0, MIDDLE),
                event("USDC", "0xccc", 0, NEWER)
        ));

        AuditResponse response = auditService.audit(
                ADDRESS,
                MIDDLE.toString(),
                NEWER.toString(),
                null,
                false
        );

        assertThat(response.events()).hasSize(2);
        assertThat(response.events().get(0).txHash()).isEqualTo("0xccc");
        assertThat(response.events().get(1).txHash()).isEqualTo("0xbbb");
    }

    @Test
    void exportCsvEscapesCommasAndQuotes() {
        when(transferEventFetcher.fetchTransferEvents(ADDRESS)).thenReturn(List.of(
                new TransferEventRecord(
                        "USDC",
                        "0xhash",
                        2,
                        ADDRESS,
                        "0x1111111111111111111111111111111111111111",
                        new BigDecimal("10"),
                        1L,
                        MIDDLE
                )
        ));

        String csv = auditService.exportCsv(ADDRESS, null, null, null, false);

        assertThat(csv).startsWith("token,tx_hash,log_index,from_address,to_address,amount,block_number,occurred_at");
        assertThat(csv).contains("USDC,0xhash,2");
    }

    private TransferEventRecord event(String token, String txHash, int logIndex, Instant occurredAt) {
        return new TransferEventRecord(
                token,
                txHash,
                logIndex,
                ADDRESS,
                "0x1111111111111111111111111111111111111111",
                new BigDecimal("1"),
                100L,
                occurredAt
        );
    }
}