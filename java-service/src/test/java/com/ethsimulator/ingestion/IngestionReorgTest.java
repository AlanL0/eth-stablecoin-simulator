package com.ethsimulator.ingestion;

import com.ethsimulator.ingestion.support.IngestionTestHarness;
import com.ethsimulator.persistence.IngestionCursor;
import com.ethsimulator.persistence.IngestionCursorRepository;
import com.ethsimulator.persistence.support.AbstractPostgresIntegrationTest;
import com.ethsimulator.protocol.AnnualizedRate;
import com.ethsimulator.protocol.BlockProvenance;
import com.ethsimulator.protocol.ProtocolAdapter;
import com.ethsimulator.protocol.ProtocolRateQuote;
import com.ethsimulator.protocol.RateConvention;
import com.ethsimulator.protocol.RateSide;
import com.ethsimulator.protocol.rpc.EthBlockHeader;
import com.ethsimulator.protocol.support.AdapterTestSupport;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IngestionReorgTest extends AbstractPostgresIntegrationTest {

    @Test
    void shortReorgRollsBackAndReplaysFromCommonAncestor() {
        long blockA = 21_000_100L;
        long blockB = blockA + 1;
        MutableBlockReader reader = new MutableBlockReader(blockB + 2);
        reader.put(blockA, hash("canon", blockA));
        reader.put(blockB, hash("canon", blockB));

        ProtocolAdapter adapter = fixtureAdapter(reader);
        IngestionProperties properties = IngestionTestHarness.testProperties();
        properties.setStartBlock(blockA);
        properties.setRangeSize(2);

        IngestionCoordinator coordinator = IngestionTestHarness.coordinator(dataSource, adapter, reader, properties);
        String sourceKey = IngestionSourceKey.forAdapter(adapter);

        coordinator.ingestSource(adapter, sourceKey, 1L);
        assertThat(priceCount(false)).isEqualTo(2);

        reader.put(blockB, hash("reorg", blockB));
        coordinator.ingestSource(adapter, sourceKey, 1L);
        assertThat(priceCount(true)).isEqualTo(1);

        coordinator.ingestSource(adapter, sourceKey, 1L);
        assertThat(priceCount(true)).isZero();
        assertThat(priceCount(false)).isEqualTo(2);
        IngestionCursor cursor = new IngestionCursorRepository(jdbcClient).find(sourceKey, 1L).orElseThrow();
        assertThat(cursor.nextBlock()).isEqualTo(blockB + 1);
    }

    @Test
    void deepReorgFailsClosedWithoutCorruptingCursor() {
        long blockA = 21_000_200L;
        long blockB = blockA + 1;
        long blockC = blockA + 2;
        long blockD = blockA + 3;
        MutableBlockReader reader = new MutableBlockReader(blockD + 2);
        for (long block : List.of(blockA, blockB, blockC, blockD)) {
            reader.put(block, hash("canon", block));
        }

        ProtocolAdapter adapter = fixtureAdapter(reader);
        IngestionProperties properties = IngestionTestHarness.testProperties();
        properties.setStartBlock(blockA);
        properties.setRangeSize(10);
        properties.setMaxReorgWindowBlocks(2);

        IngestionCoordinator coordinator = IngestionTestHarness.coordinator(dataSource, adapter, reader, properties);
        String sourceKey = IngestionSourceKey.forAdapter(adapter);
        coordinator.ingestSource(adapter, sourceKey, 1L);

        for (long block : List.of(blockA, blockB, blockC, blockD)) {
            reader.put(block, hash("reorg", block));
        }

        assertThatThrownBy(() -> coordinator.ingestSource(adapter, sourceKey, 1L))
                .isInstanceOf(DeepReorgException.class);

        IngestionCursor cursor = new IngestionCursorRepository(jdbcClient).find(sourceKey, 1L).orElseThrow();
        assertThat(cursor.nextBlock()).isEqualTo(blockD + 1);
        assertThat(healthStatus(sourceKey)).isEqualTo("unavailable");
    }

    private long priceCount(boolean reverted) {
        return jdbcClient.sql("select count(*) from price_observations where source = 'chainlink' and is_reverted = ?")
                .param(reverted)
                .query(Long.class)
                .single();
    }

    private String healthStatus(String sourceKey) {
        return jdbcClient.sql("select status from source_health where source_key = ?")
                .param(sourceKey)
                .query(String.class)
                .single();
    }

    private ProtocolAdapter fixtureAdapter(MutableBlockReader reader) {
        return new ProtocolAdapter() {
            @Override
            public String protocolId() {
                return "chainlink";
            }

            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public List<ProtocolRateQuote> fetchQuotes() {
                return fetchQuotesAtBlock(reader.latestBlockNumber());
            }

            @Override
            public List<ProtocolRateQuote> fetchQuotesAtBlock(long blockNumber) {
                EthBlockHeader header = reader.blockAt(blockNumber);
                Instant observedAt = IngestionTestHarness.fixedClock(AdapterTestSupport.FIXTURE_TIMESTAMP).instant();
                return List.of(ProtocolRateQuote.available(
                        "chainlink",
                        "ETH/USD",
                        RateSide.PRICE,
                        AnnualizedRate.of(FinancialMath.bd("3500"), RateConvention.SPOT_USD, "fixture"),
                        "latest",
                        "0xfeed",
                        new BlockProvenance(1L, blockNumber, header.hash(), header.timestamp()),
                        observedAt,
                        observedAt,
                        false
                ));
            }
        };
    }

    private static String hash(String prefix, long block) {
        return String.format("0x%s%058x", prefix, block);
    }

    private static final class MutableBlockReader implements FinalizedBlockReader {
        private final long latest;
        private final Map<Long, String> hashes = new ConcurrentHashMap<>();

        private MutableBlockReader(long latest) {
            this.latest = latest;
        }

        void put(long block, String hash) {
            hashes.put(block, hash);
        }

        @Override
        public long latestBlockNumber() {
            return latest;
        }

        @Override
        public EthBlockHeader blockAt(long blockNumber) {
            return new EthBlockHeader(
                    BigInteger.valueOf(blockNumber),
                    hashes.get(blockNumber),
                    IngestionTestHarness.fixedClock(AdapterTestSupport.FIXTURE_TIMESTAMP).instant()
            );
        }
    }
}