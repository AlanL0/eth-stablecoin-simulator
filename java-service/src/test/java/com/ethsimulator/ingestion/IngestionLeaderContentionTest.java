package com.ethsimulator.ingestion;

import com.ethsimulator.ingestion.support.IngestionTestHarness;
import com.ethsimulator.ingestion.support.MultiBlockJsonRpcFixtureServer;
import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import com.ethsimulator.protocol.abi.ProtocolAbi;
import com.ethsimulator.protocol.chainlink.ChainlinkAdapter;
import com.ethsimulator.protocol.support.AbiResponseEncoder;
import com.ethsimulator.protocol.support.AdapterTestSupport;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionLeaderContentionTest extends AbstractPostgresIntegrationTest {

    @Test
    void onlyOneCoordinatorAdvancesCursorUnderLeaseContention() {
        long head = 21_000_030L;
        long block = head - 2;
        IngestionProperties properties = IngestionTestHarness.testProperties();
        properties.setStartBlock(block);

        try (MultiBlockJsonRpcFixtureServer rpc = fixture(head, block);
             HikariDataSource leaderA = dedicatedDataSource();
             HikariDataSource leaderB = dedicatedDataSource()) {
            ChainlinkAdapter adapter = adapter(rpc.rpcUrl());
            var reader = IngestionTestHarness.web3jBlockReader(rpc.rpcUrl());

            IngestionCoordinator coordinatorA = IngestionTestHarness.coordinator(leaderA, adapter, reader, properties);
            IngestionCoordinator coordinatorB = IngestionTestHarness.coordinator(leaderB, adapter, reader, properties);

            coordinatorA.runCycle("worker-a");
            coordinatorB.runCycle("worker-b");

            long rows = JdbcClient.create(dataSource)
                    .sql("select count(*) from price_observations where source = 'chainlink'")
                    .query(Long.class)
                    .single();
            long nextBlock = JdbcClient.create(dataSource)
                    .sql("select next_block from ingestion_cursors where source_key = 'protocol:chainlink'")
                    .query(Long.class)
                    .single();

            assertThat(rows).isEqualTo(1);
            assertThat(nextBlock).isEqualTo(block + 1);
        }
    }

    private HikariDataSource dedicatedDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(POSTGRES.getJdbcUrl());
        ds.setUsername(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        ds.setMaximumPoolSize(1);
        return ds;
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