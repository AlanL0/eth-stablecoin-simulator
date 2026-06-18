package com.ethsimulator.protocol.aave;

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

class AaveAdapterTest {

    @Test
    void fixedBlockFixtureReadsGhoBorrowAndSghoSavings() {
        ProtocolSourcesProperties properties = AdapterTestSupport.defaultProperties();
        String pool = properties.getAave().getPool();
        String gho = properties.getAave().getGho();
        String sgho = properties.getAave().getSgho();

        try (JsonRpcFixtureServer rpc = new JsonRpcFixtureServer(
                AdapterTestSupport.FIXTURE_BLOCK,
                AdapterTestSupport.FIXTURE_TIMESTAMP,
                AdapterTestSupport.FIXTURE_BLOCK_HASH
        )) {
            rpc.registerEthCall(pool, ProtocolAbi.selector(ProtocolAbi.getReserveData(gho)),
                            AbiResponseEncoder.aaveReserveData(new BigInteger("50000000000000000000000000")))
                    .registerEthCall(sgho, ProtocolAbi.selector(ProtocolAbi.TARGET_RATE),
                            AbiResponseEncoder.uint256(new BigInteger("40000000000000000000000000")))
                    .start();

            AaveAdapter adapter = new AaveAdapter(
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
            assertEquals(FinancialMath.bd("0.04000000"), savings.rate().value());
            assertEquals(pool.toLowerCase(), borrow.sourceContract().toLowerCase());
            assertEquals(sgho.toLowerCase(), savings.sourceContract().toLowerCase());
            assertTrue(borrow.rate().methodology().contains("variableBorrowRate"));
            assertTrue(savings.rate().methodology().contains("targetRate"));
        }
    }

    @Test
    void returnsUnavailableWhenSghoReverts() {
        ProtocolSourcesProperties properties = AdapterTestSupport.defaultProperties();
        String pool = properties.getAave().getPool();
        String gho = properties.getAave().getGho();
        String sgho = properties.getAave().getSgho();

        try (JsonRpcFixtureServer rpc = new JsonRpcFixtureServer(
                AdapterTestSupport.FIXTURE_BLOCK,
                AdapterTestSupport.FIXTURE_TIMESTAMP,
                AdapterTestSupport.FIXTURE_BLOCK_HASH
        )) {
            rpc.registerEthCall(pool, ProtocolAbi.selector(ProtocolAbi.getReserveData(gho)),
                            AbiResponseEncoder.aaveReserveData(new BigInteger("50000000000000000000000000")))
                    .registerEthCall(sgho, ProtocolAbi.selector(ProtocolAbi.TARGET_RATE), "0x")
                    .start();

            AaveAdapter adapter = new AaveAdapter(
                    properties,
                    AdapterTestSupport.ethCallClient(rpc.rpcUrl()),
                    AdapterTestSupport.fixedClock()
            );

            ProtocolRateQuote savings = adapter.fetchQuotes().stream()
                    .filter(quote -> quote.side() == RateSide.SAVINGS)
                    .findFirst()
                    .orElseThrow();

            assertTrue(savings.unavailableReasonOptional().isPresent());
            assertTrue(savings.reverted());
        }
    }
}