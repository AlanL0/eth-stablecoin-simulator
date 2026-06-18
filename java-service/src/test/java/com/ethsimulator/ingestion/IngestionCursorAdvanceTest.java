package com.ethsimulator.ingestion;

import com.ethsimulator.ingestion.support.IngestionTestHarness;
import com.ethsimulator.ingestion.support.MultiBlockJsonRpcFixtureServer;
import com.ethsimulator.persistence.IngestionCursor;
import com.ethsimulator.persistence.IngestionCursorRepository;
import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import com.ethsimulator.protocol.abi.ProtocolAbi;
import com.ethsimulator.protocol.chainlink.ChainlinkAdapter;
import com.ethsimulator.protocol.support.AbiResponseEncoder;
import com.ethsimulator.protocol.support.AdapterTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionCursorAdvanceTest extends AbstractPostgresIntegrationTest {

    @Test
    void cursorAdvancesWithFinalizedBlockMetadata() {
        long head = 21_000_020L;
        long block = head - 2;
        IngestionProperties properties = IngestionTestHarness.testProperties();
        properties.setStartBlock(block);

        try (MultiBlockJsonRpcFixtureServer rpc = fixture(head, block)) {
            ChainlinkAdapter adapter = adapter(rpc.rpcUrl());
            IngestionCoordinator coordinator = IngestionTestHarness.coordinator(
                    dataSource,
                    adapter,
                    IngestionTestHarness.web3jBlockReader(rpc.rpcUrl()),
                    properties
            );

            coordinator.ingestSource(adapter, IngestionSourceKey.forAdapter(adapter), 1L);

            IngestionCursorRepository repository = new IngestionCursorRepository(jdbcClient);
            IngestionCursor cursor = repository.find("protocol:chainlink", 1L).orElseThrow();

            assertThat(cursor.nextBlock()).isEqualTo(block + 1);
            assertThat(cursor.lastFinalizedBlock()).isEqualTo(block);
            assertThat(cursor.lastFinalizedBlockHash()).isEqualTo(hash(block));
        }
    }

    private ChainlinkAdapter adapter(String rpcUrl) {
        return new ChainlinkAdapter(
                AdapterTestSupport.defaultProperties(),
                AdapterTestSupport.ethCallClient(rpcUrl),
                IngestionTestHarness.fixedClock(AdapterTestSupport.FIXTURE_TIMESTAMP)
        );
    }

    private MultiBlockJsonRpcFixtureServer fixture(long head, long block) {
        String feed = AdapterTestSupport.defaultProperties().getChainlink().getAddress();
        return new MultiBlockJsonRpcFixtureServer(head)
                .registerBlock(block, hash(block), AdapterTestSupport.FIXTURE_TIMESTAMP)
                .registerEthCall(feed, ProtocolAbi.selector(ProtocolAbi.LATEST_ROUND_DATA),
                        AbiResponseEncoder.latestRoundData(100, 3_500_000_000_00L, 0, AdapterTestSupport.FIXTURE_TIMESTAMP, 100))
                .registerEthCall(feed, ProtocolAbi.selector(ProtocolAbi.DECIMALS), AbiResponseEncoder.uint8(8))
                .start();
    }

    private static String hash(long block) {
        return String.format("0x%064x", block);
    }
}