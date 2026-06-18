package com.ethsimulator.protocol.chainlink;

import com.ethsimulator.protocol.ProtocolRateQuote;
import com.ethsimulator.protocol.ProtocolSourcesProperties;
import com.ethsimulator.protocol.RateConvention;
import com.ethsimulator.protocol.RateSide;
import com.ethsimulator.protocol.abi.ProtocolAbi;
import com.ethsimulator.protocol.support.AbiResponseEncoder;
import com.ethsimulator.protocol.support.AdapterTestSupport;
import com.ethsimulator.protocol.support.JsonRpcFixtureServer;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;
import org.web3j.abi.FunctionEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainlinkAdapterTest {

    @Test
    void fixedBlockFixtureIncludesProvenanceAndRejectsStaleRound() {
        ProtocolSourcesProperties properties = AdapterTestSupport.defaultProperties();
        String feed = properties.getChainlink().getAddress();

        try (JsonRpcFixtureServer rpc = new JsonRpcFixtureServer(
                AdapterTestSupport.FIXTURE_BLOCK,
                AdapterTestSupport.FIXTURE_TIMESTAMP,
                AdapterTestSupport.FIXTURE_BLOCK_HASH
        )) {
            rpc.registerEthCall(feed, ProtocolAbi.selector(ProtocolAbi.LATEST_ROUND_DATA),
                            AbiResponseEncoder.latestRoundData(100, 3_500_000_000_00L, 0, AdapterTestSupport.FIXTURE_TIMESTAMP - 7200, 100))
                    .registerEthCall(feed, ProtocolAbi.selector(ProtocolAbi.DECIMALS), AbiResponseEncoder.uint8(8))
                    .registerEthCall(feed, FunctionEncoder.encode(ProtocolAbi.getRoundData(99)),
                            AbiResponseEncoder.latestRoundData(99, 3_400_000_000_00L, 0, AdapterTestSupport.FIXTURE_TIMESTAMP - 10_000, 99))
                    .start();

            ChainlinkAdapter adapter = new ChainlinkAdapter(
                    properties,
                    AdapterTestSupport.ethCallClient(rpc.rpcUrl()),
                    AdapterTestSupport.fixedClock()
            );

            List<ProtocolRateQuote> quotes = adapter.fetchQuotes();
            assertTrue(quotes.size() >= 1);

            ProtocolRateQuote latest = quotes.getFirst();
            assertEquals("chainlink", latest.protocol());
            assertEquals(RateSide.PRICE, latest.side());
            assertEquals(FinancialMath.bd("3500.00"), latest.rate().value());
            assertEquals(RateConvention.SPOT_USD, latest.rate().convention());
            assertEquals(feed.toLowerCase(), latest.sourceContract().toLowerCase());
            assertEquals(AdapterTestSupport.FIXTURE_BLOCK, latest.provenance().blockNumber());
            assertEquals(AdapterTestSupport.FIXTURE_BLOCK_HASH, latest.provenance().blockHash());
            assertTrue(latest.stale());
            assertFalse(latest.reverted());

            if (quotes.size() > 1) {
                assertEquals("round:99", quotes.get(1).lookbackWindow());
            }
        }
    }

    @Test
    void returnsUnavailableOnRevertedEthCall() {
        ProtocolSourcesProperties properties = AdapterTestSupport.defaultProperties();
        String feed = properties.getChainlink().getAddress();

        try (JsonRpcFixtureServer rpc = new JsonRpcFixtureServer(
                AdapterTestSupport.FIXTURE_BLOCK,
                AdapterTestSupport.FIXTURE_TIMESTAMP,
                AdapterTestSupport.FIXTURE_BLOCK_HASH
        )) {
            rpc.registerEthCall(feed, ProtocolAbi.selector(ProtocolAbi.LATEST_ROUND_DATA), "0x")
                    .start();

            ChainlinkAdapter adapter = new ChainlinkAdapter(
                    properties,
                    AdapterTestSupport.ethCallClient(rpc.rpcUrl()),
                    AdapterTestSupport.fixedClock()
            );

            ProtocolRateQuote quote = adapter.fetchQuotes().get(0);
            assertTrue(quote.unavailableReasonOptional().isPresent());
            assertTrue(quote.reverted());
        }
    }
}