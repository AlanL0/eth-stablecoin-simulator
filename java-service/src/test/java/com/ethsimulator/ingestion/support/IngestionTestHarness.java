package com.ethsimulator.ingestion.support;

import com.ethsimulator.config.BlockingIoExecutor;
import com.ethsimulator.ingestion.FinalizedBlockReader;
import com.ethsimulator.ingestion.IngestionCoordinator;
import com.ethsimulator.ingestion.IngestionMetrics;
import com.ethsimulator.ingestion.IngestionPersistenceService;
import com.ethsimulator.ingestion.IngestionProperties;
import com.ethsimulator.ingestion.IngestionReorgHandler;
import com.ethsimulator.ingestion.IngestionSourceKey;
import com.ethsimulator.persistence.IngestionLeaderLock;
import com.ethsimulator.protocol.ProtocolAdapter;
import com.ethsimulator.protocol.ProtocolSourcesProperties;
import com.ethsimulator.protocol.rpc.EthBlockHeader;
import com.ethsimulator.protocol.rpc.EthCallClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class IngestionTestHarness {

    private IngestionTestHarness() {
    }

    public static PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    public static IngestionCoordinator coordinator(
            DataSource dataSource,
            ProtocolAdapter adapter,
            FinalizedBlockReader blockReader,
            IngestionProperties properties
    ) {
        JdbcClient jdbcClient = JdbcClient.create(dataSource);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager(dataSource));
        IngestionPersistenceService persistence = new IngestionPersistenceService(
                jdbcClient,
                transactionTemplate,
                new com.ethsimulator.persistence.IngestionCursorRepository(jdbcClient),
                new com.ethsimulator.persistence.PriceObservationRepository(jdbcClient),
                new com.ethsimulator.persistence.RateObservationRepository(jdbcClient),
                new com.ethsimulator.persistence.SourceHealthRepository(jdbcClient)
        );
        IngestionReorgHandler reorgHandler = new IngestionReorgHandler(properties, blockReader, persistence);
        BlockingIoExecutor executor = new BlockingIoExecutor(
                Executors.newVirtualThreadPerTaskExecutor(),
                8
        );
        ProtocolSourcesProperties protocolSources = new ProtocolSourcesProperties();
        return new IngestionCoordinator(
                properties,
                protocolSources,
                List.of(adapter),
                blockReader,
                new IngestionLeaderLock(jdbcClient),
                persistence,
                reorgHandler,
                new IngestionMetrics(new SimpleMeterRegistry()),
                executor
        );
    }

    public static FinalizedBlockReader web3jBlockReader(String rpcUrl) {
        EthCallClient client = new EthCallClient(Web3j.build(new HttpService(rpcUrl)));
        return new FinalizedBlockReader() {
            @Override
            public long latestBlockNumber() {
                return client.latestBlock().number().longValue();
            }

            @Override
            public EthBlockHeader blockAt(long blockNumber) {
                return client.blockAt(BigInteger.valueOf(blockNumber));
            }
        };
    }

    public static IngestionProperties testProperties() {
        IngestionProperties properties = new IngestionProperties();
        properties.setConfirmationDepth(2);
        properties.setRangeSize(5);
        properties.setMaxCatchUpBlocks(10);
        properties.setMaxReorgWindowBlocks(8);
        properties.setMaxRetries(2);
        properties.setRetryBackoffMs(1);
        properties.setRequestTimeoutMs(5_000);
        properties.setLeaseDurationSeconds(30);
        return properties;
    }

    public static Clock fixedClock(long epochSecond) {
        return Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);
    }

    public static String sourceKey(ProtocolAdapter adapter) {
        return IngestionSourceKey.forAdapter(adapter);
    }

    public static Map<Long, String> blockHashes(long from, long through, String prefix) {
        java.util.LinkedHashMap<Long, String> hashes = new java.util.LinkedHashMap<>();
        for (long block = from; block <= through; block++) {
            hashes.put(block, prefix + block);
        }
        return hashes;
    }
}