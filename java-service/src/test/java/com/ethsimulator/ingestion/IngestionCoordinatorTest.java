package com.ethsimulator.ingestion;

import com.ethsimulator.ingestion.support.IngestionTestHarness;
import com.ethsimulator.ingestion.support.MultiBlockJsonRpcFixtureServer;
import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import com.ethsimulator.protocol.ProtocolSourcesProperties;
import com.ethsimulator.protocol.abi.ProtocolAbi;
import com.ethsimulator.protocol.chainlink.ChainlinkAdapter;
import com.ethsimulator.protocol.support.AbiResponseEncoder;
import com.ethsimulator.protocol.support.AdapterTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionCoordinatorTest extends AbstractPostgresIntegrationTest {

    private static final long HEAD = 21_000_010L;
    private static final long FINALIZED = HEAD - 2;

    @Test
    void emptyStartIngestsFromConfiguredStartBlock() {
        IngestionProperties properties = IngestionTestHarness.testProperties();
        properties.setStartBlock(FINALIZED);

        try (MultiBlockJsonRpcFixtureServer rpc = chainlinkFixture(FINALIZED, FINALIZED)) {
            ChainlinkAdapter adapter = chainlinkAdapter(rpc.rpcUrl());
            IngestionCoordinator coordinator = IngestionTestHarness.coordinator(
                    dataSource,
                    adapter,
                    IngestionTestHarness.web3jBlockReader(rpc.rpcUrl()),
                    properties
            );

            coordinator.ingestSource(adapter, IngestionSourceKey.forAdapter(adapter), 1L);

            assertThat(cursorNextBlock()).isEqualTo(FINALIZED + 1);
            assertThat(priceRows()).isEqualTo(1);
        }
    }

    @Test
    void restartResumesWithoutDuplicatingRows() {
        IngestionProperties properties = IngestionTestHarness.testProperties();
        properties.setStartBlock(FINALIZED);

        try (MultiBlockJsonRpcFixtureServer rpc = chainlinkFixture(FINALIZED, FINALIZED)) {
            ChainlinkAdapter adapter = chainlinkAdapter(rpc.rpcUrl());
            FinalizedBlockReader reader = IngestionTestHarness.web3jBlockReader(rpc.rpcUrl());
            IngestionCoordinator coordinator = IngestionTestHarness.coordinator(dataSource, adapter, reader, properties);

            coordinator.ingestSource(adapter, IngestionSourceKey.forAdapter(adapter), 1L);
            coordinator.ingestSource(adapter, IngestionSourceKey.forAdapter(adapter), 1L);

            assertThat(priceRows()).isEqualTo(1);
            assertThat(cursorNextBlock()).isEqualTo(FINALIZED + 1);
        }
    }

    @Test
    void duplicateNotificationIsIdempotent() {
        IngestionProperties properties = IngestionTestHarness.testProperties();
        properties.setStartBlock(FINALIZED);

        try (MultiBlockJsonRpcFixtureServer rpc = chainlinkFixture(FINALIZED, FINALIZED)) {
            ChainlinkAdapter adapter = chainlinkAdapter(rpc.rpcUrl());
            IngestionCoordinator coordinator = IngestionTestHarness.coordinator(
                    dataSource,
                    adapter,
                    IngestionTestHarness.web3jBlockReader(rpc.rpcUrl()),
                    properties
            );

            String sourceKey = IngestionSourceKey.forAdapter(adapter);
            coordinator.ingestSource(adapter, sourceKey, 1L);
            new IngestionWakeSignal().signalNewBlock("0x" + Long.toHexString(HEAD));
            coordinator.ingestSource(adapter, sourceKey, 1L);

            assertThat(priceRows()).isEqualTo(1);
        }
    }

    @Test
    void skippedRangeCatchUpProcessesBoundedChunks() {
        IngestionProperties properties = IngestionTestHarness.testProperties();
        properties.setStartBlock(FINALIZED - 3);
        properties.setRangeSize(2);

        try (MultiBlockJsonRpcFixtureServer rpc = chainlinkFixture(FINALIZED - 3, FINALIZED)) {
            ChainlinkAdapter adapter = chainlinkAdapter(rpc.rpcUrl());
            IngestionCoordinator coordinator = IngestionTestHarness.coordinator(
                    dataSource,
                    adapter,
                    IngestionTestHarness.web3jBlockReader(rpc.rpcUrl()),
                    properties
            );

            IngestionCycleResult first = coordinator.ingestSource(
                    adapter,
                    IngestionSourceKey.forAdapter(adapter),
                    1L
            );
            IngestionCycleResult second = coordinator.ingestSource(
                    adapter,
                    IngestionSourceKey.forAdapter(adapter),
                    1L
            );

            assertThat(first.blocksIngested()).isEqualTo(2);
            assertThat(second.blocksIngested()).isGreaterThanOrEqualTo(1);
            assertThat(cursorNextBlock()).isEqualTo(FINALIZED + 1);
            assertThat(priceRows()).isEqualTo(4);
        }
    }

    @Test
    void catchUpCapLimitsBlocksPerCycle() {
        IngestionProperties properties = IngestionTestHarness.testProperties();
        properties.setStartBlock(FINALIZED - 5);
        properties.setRangeSize(20);
        properties.setMaxCatchUpBlocks(2);

        try (MultiBlockJsonRpcFixtureServer rpc = chainlinkFixture(FINALIZED - 5, FINALIZED)) {
            ChainlinkAdapter adapter = chainlinkAdapter(rpc.rpcUrl());
            IngestionCoordinator coordinator = IngestionTestHarness.coordinator(
                    dataSource,
                    adapter,
                    IngestionTestHarness.web3jBlockReader(rpc.rpcUrl()),
                    properties
            );

            IngestionCycleResult result = coordinator.ingestSource(
                    adapter,
                    IngestionSourceKey.forAdapter(adapter),
                    1L
            );

            assertThat(result.blocksIngested()).isEqualTo(2);
            assertThat(cursorNextBlock()).isEqualTo(FINALIZED - 3);
        }
    }

    @Test
    void partialFailureRecordsDegradedHealthWithoutCorruptingFreshRows() {
        IngestionProperties properties = IngestionTestHarness.testProperties();
        properties.setStartBlock(FINALIZED);

        try (MultiBlockJsonRpcFixtureServer rpc = new MultiBlockJsonRpcFixtureServer(HEAD)
                .registerBlock(FINALIZED, hash(FINALIZED), AdapterTestSupport.FIXTURE_TIMESTAMP)
                .registerEthCall(
                        AdapterTestSupport.defaultProperties().getChainlink().getAddress(),
                        ProtocolAbi.selector(ProtocolAbi.LATEST_ROUND_DATA),
                        "0x"
                )
                .start()) {
            ChainlinkAdapter adapter = chainlinkAdapter(rpc.rpcUrl());
            IngestionCoordinator coordinator = IngestionTestHarness.coordinator(
                    dataSource,
                    adapter,
                    IngestionTestHarness.web3jBlockReader(rpc.rpcUrl()),
                    properties
            );

            coordinator.ingestSource(adapter, IngestionSourceKey.forAdapter(adapter), 1L);

            assertThat(priceRows()).isZero();
            assertThat(healthStatus()).isEqualTo("degraded");
            assertThat(cursorNextBlock()).isEqualTo(FINALIZED + 1);
        }
    }

    @Test
    void adapterAndJdbcBoundariesRunOnVirtualThreads() {
        IngestionProperties properties = IngestionTestHarness.testProperties();
        properties.setStartBlock(FINALIZED);

        try (MultiBlockJsonRpcFixtureServer rpc = chainlinkFixture(FINALIZED, FINALIZED)) {
            VirtualThreadTrackingAdapter adapter = new VirtualThreadTrackingAdapter(chainlinkAdapter(rpc.rpcUrl()));
            AtomicBoolean jdbcVirtual = new AtomicBoolean(false);
            IngestionCoordinator coordinator = new IngestionCoordinator(
                    properties,
                    new ProtocolSourcesProperties(),
                    java.util.List.of(adapter),
                    IngestionTestHarness.web3jBlockReader(rpc.rpcUrl()),
                    new com.ethsimulator.persistence.IngestionLeaderLock(jdbcClient),
                    new IngestionPersistenceService(
                            jdbcClient,
                            new org.springframework.transaction.support.TransactionTemplate(
                                    IngestionTestHarness.transactionManager(dataSource)
                            ),
                            new com.ethsimulator.persistence.IngestionCursorRepository(jdbcClient),
                            new com.ethsimulator.persistence.PriceObservationRepository(jdbcClient),
                            new com.ethsimulator.persistence.RateObservationRepository(jdbcClient),
                            new com.ethsimulator.persistence.SourceHealthRepository(jdbcClient)
                    ) {
                        @Override
                        public void commitBlock(
                                String sourceKey,
                                long chainId,
                                long blockNumber,
                                String blockHash,
                                java.util.List<com.ethsimulator.persistence.PriceObservation> prices,
                                java.util.List<com.ethsimulator.persistence.RateObservation> rates,
                                com.ethsimulator.persistence.SourceHealth health
                        ) {
                            jdbcVirtual.set(Thread.currentThread().isVirtual());
                            super.commitBlock(sourceKey, chainId, blockNumber, blockHash, prices, rates, health);
                        }
                    },
                    new IngestionReorgHandler(
                            properties,
                            IngestionTestHarness.web3jBlockReader(rpc.rpcUrl()),
                            new IngestionPersistenceService(
                                    jdbcClient,
                                    new org.springframework.transaction.support.TransactionTemplate(
                                            IngestionTestHarness.transactionManager(dataSource)
                                    ),
                                    new com.ethsimulator.persistence.IngestionCursorRepository(jdbcClient),
                                    new com.ethsimulator.persistence.PriceObservationRepository(jdbcClient),
                                    new com.ethsimulator.persistence.RateObservationRepository(jdbcClient),
                                    new com.ethsimulator.persistence.SourceHealthRepository(jdbcClient)
                            )
                    ),
                    new IngestionMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
                    new com.ethsimulator.config.BlockingIoExecutor(
                            java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor(),
                            8
                    )
            );

            coordinator.ingestSource(adapter, IngestionSourceKey.forAdapter(adapter), 1L);

            assertThat(adapter.adapterRanOnVirtualThread()).isTrue();
            assertThat(jdbcVirtual).isTrue();
        }
    }

    private ChainlinkAdapter chainlinkAdapter(String rpcUrl) {
        ProtocolSourcesProperties properties = AdapterTestSupport.defaultProperties();
        return new ChainlinkAdapter(
                properties,
                AdapterTestSupport.ethCallClient(rpcUrl),
                IngestionTestHarness.fixedClock(AdapterTestSupport.FIXTURE_TIMESTAMP)
        );
    }

    private MultiBlockJsonRpcFixtureServer chainlinkFixture(long fromBlock, long throughBlock) {
        ProtocolSourcesProperties properties = AdapterTestSupport.defaultProperties();
        String feed = properties.getChainlink().getAddress();

        MultiBlockJsonRpcFixtureServer rpc = new MultiBlockJsonRpcFixtureServer(HEAD)
                .registerBlock(HEAD, hash(HEAD), AdapterTestSupport.FIXTURE_TIMESTAMP);
        for (long block = fromBlock; block <= throughBlock; block++) {
            rpc.registerBlock(block, hash(block), AdapterTestSupport.FIXTURE_TIMESTAMP)
                    .registerEthCall(feed, ProtocolAbi.selector(ProtocolAbi.LATEST_ROUND_DATA),
                            AbiResponseEncoder.latestRoundData(
                                    100,
                                    3_500_000_000_00L,
                                    0,
                                    AdapterTestSupport.FIXTURE_TIMESTAMP,
                                    100
                            ))
                    .registerEthCall(feed, ProtocolAbi.selector(ProtocolAbi.DECIMALS), AbiResponseEncoder.uint8(8));
        }
        return rpc.start();
    }

    private long cursorNextBlock() {
        return JdbcClient.create(dataSource)
                .sql("select next_block from ingestion_cursors where source_key = ?")
                .param("protocol:chainlink")
                .query(Long.class)
                .single();
    }

    private long priceRows() {
        return JdbcClient.create(dataSource)
                .sql("select count(*) from price_observations where source = 'chainlink'")
                .query(Long.class)
                .single();
    }

    private String healthStatus() {
        return JdbcClient.create(dataSource)
                .sql("select status from source_health where source_key = ?")
                .param("protocol:chainlink")
                .query(String.class)
                .single();
    }

    private static String hash(long block) {
        return String.format("0x%064x", block);
    }
}