package com.ethsimulator.protocol.sky;

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

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyAdapterTest {

    @Test
    void fixedBlockFixtureDerivesBorrowAndSavingsFromJugAndSusds() {
        ProtocolSourcesProperties properties = AdapterTestSupport.defaultProperties();
        String jug = properties.getSky().getJug();
        String susds = properties.getSky().getSusds();

        BigInteger duty = FinancialMath.RAY.toBigInteger().add(new BigInteger("1000000000000000000"));
        BigInteger base = FinancialMath.RAY.toBigInteger();
        BigInteger ssr = FinancialMath.RAY.toBigInteger().add(new BigInteger("2000000000000000000"));

        try (JsonRpcFixtureServer rpc = new JsonRpcFixtureServer(
                AdapterTestSupport.FIXTURE_BLOCK,
                AdapterTestSupport.FIXTURE_TIMESTAMP,
                AdapterTestSupport.FIXTURE_BLOCK_HASH
        )) {
            rpc.registerEthCall(properties.getSky().getChainlog(),
                            ProtocolAbi.selector(ProtocolAbi.chainlogGetAddress("MCD_JUG")),
                            AbiResponseEncoder.address(jug))
                    .registerEthCall(jug, ProtocolAbi.selector(ProtocolAbi.JUG_BASE), AbiResponseEncoder.uint256(base))
                    .registerEthCall(jug, ProtocolAbi.selector(ProtocolAbi.jugIlks("ETH-A")),
                            AbiResponseEncoder.encode(java.util.List.of(duty, BigInteger.ONE)))
                    .registerEthCall(susds, ProtocolAbi.selector(ProtocolAbi.SUSDS_SSR), AbiResponseEncoder.uint256(ssr))
                    .start();

            SkyAdapter adapter = new SkyAdapter(
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

            assertEquals(RateConvention.APR_EFFECTIVE, borrow.rate().convention());
            assertTrue(borrow.rate().methodology().contains("jug"));
            assertEquals(RateConvention.APR_EFFECTIVE, savings.rate().convention());
            assertEquals(susds.toLowerCase(), savings.sourceContract().toLowerCase());
            assertEquals(AdapterTestSupport.FIXTURE_BLOCK, savings.provenance().blockNumber());
        }
    }
}