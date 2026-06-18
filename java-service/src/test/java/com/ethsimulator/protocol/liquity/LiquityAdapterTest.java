package com.ethsimulator.protocol.liquity;

import com.ethsimulator.protocol.ProtocolRateQuote;
import com.ethsimulator.protocol.ProtocolSourcesProperties;
import com.ethsimulator.protocol.RateSide;
import com.ethsimulator.protocol.abi.ProtocolAbi;
import com.ethsimulator.protocol.support.AbiResponseEncoder;
import com.ethsimulator.protocol.support.AdapterTestSupport;
import com.ethsimulator.protocol.support.JsonRpcFixtureServer;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiquityAdapterTest {

    @Test
    void fixedBlockFixtureDerivesWeightedBorrowAprAndTrailingSavings() {
        ProtocolSourcesProperties properties = AdapterTestSupport.defaultProperties();
        String activePool = properties.getLiquity().getWethActivePool();
        String stabilityPool = properties.getLiquity().getWethStabilityPool();
        String boldToken = properties.getLiquity().getBoldToken();

        BigInteger weighted = new BigInteger("5000000000000000000");
        BigInteger recorded = new BigInteger("100000000000000000000");

        try (JsonRpcFixtureServer rpc = new JsonRpcFixtureServer(
                AdapterTestSupport.FIXTURE_BLOCK,
                AdapterTestSupport.FIXTURE_TIMESTAMP,
                AdapterTestSupport.FIXTURE_BLOCK_HASH
        )) {
            rpc.registerEthCall(activePool, ProtocolAbi.selector(ProtocolAbi.AGG_WEIGHTED_DEBT_SUM),
                            AbiResponseEncoder.uint256(weighted))
                    .registerEthCall(activePool, ProtocolAbi.selector(ProtocolAbi.AGG_RECORDED_DEBT),
                            AbiResponseEncoder.uint256(recorded))
                    .registerEthCall(stabilityPool, ProtocolAbi.selector(ProtocolAbi.GET_TOTAL_BOLD_DEPOSITS),
                            AbiResponseEncoder.uint256(new BigInteger("1000000000000000000000")))
                    .start();

            rpc.stubTransferLogs(JsonRpcFixtureServer.logsArray(java.util.List.of(
                    JsonRpcFixtureServer.transferLog(
                            boldToken,
                            activePool,
                            stabilityPool,
                            new BigInteger("1000000000000000000"),
                            AdapterTestSupport.FIXTURE_BLOCK - 10
                    )
            )));

            LiquityAdapter adapter = new LiquityAdapter(
                    properties,
                    AdapterTestSupport.ethCallClient(rpc.rpcUrl()),
                    AdapterTestSupport.fixedClock()
            );

            ProtocolRateQuote borrow = adapter.fetchQuotes().stream()
                    .filter(quote -> quote.side() == RateSide.BORROW)
                    .findFirst()
                    .orElseThrow();
            ProtocolRateQuote savings = adapter.fetchQuotes().stream()
                    .filter(quote -> quote.side() == RateSide.SAVINGS)
                    .findFirst()
                    .orElseThrow();

            assertEquals(FinancialMath.bd("0.05000000"), borrow.rate().value());
            assertTrue(savings.rate().methodology().contains("excludes liquidation collateral gains"));
            assertEquals("30d", savings.lookbackWindow());
        }
    }
}