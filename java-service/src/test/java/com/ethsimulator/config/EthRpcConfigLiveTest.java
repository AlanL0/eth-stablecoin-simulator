package com.ethsimulator.config;

import com.ethsimulator.blockchain.ChainlinkEthUsdReader;
import com.ethsimulator.market.EthPriceQuote;
import com.ethsimulator.market.EthPriceService;
import com.ethsimulator.market.EthPriceSource;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;
import org.web3j.protocol.Web3j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ETH_RPC_URL", matches = ".+")
class EthRpcConfigLiveTest {

    @Autowired
    private EthSimulatorProperties properties;

    @Autowired
    private ObjectProvider<Web3j> web3jProvider;

    @Autowired
    private ChainlinkEthUsdReader chainlinkEthUsdReader;

    @Autowired
    private EthPriceService ethPriceService;

    @Test
    void ethRpcUrlBoundFromEnvironmentWhenPresent() {
        String env = System.getenv("ETH_RPC_URL");
        Assumptions.assumeTrue(StringUtils.hasText(env), "ETH_RPC_URL not set in environment");

        assertTrue(StringUtils.hasText(properties.getEthRpcUrl()), "eth-simulator.eth-rpc-url should be bound");
        assertEquals(env.trim(), properties.getEthRpcUrl().trim());
        Web3j web3j = web3jProvider.getIfAvailable();
        assertTrue(web3j != null, "Web3j bean should exist when eth-rpc-url is set");
        assertTrue(StringUtils.hasText(properties.getChainlinkEthUsdFeed()));
        assertTrue(chainlinkEthUsdReader.readPriceUsd().isPresent(),
                () -> "Chainlink read failed for feed " + properties.getChainlinkEthUsdFeed()
                        + " via reader " + chainlinkEthUsdReader.getClass().getSimpleName());

        EthPriceQuote quote = ethPriceService.currentPrice();
        assertEquals(EthPriceSource.CHAINLINK, quote.source(), "expected Chainlink with live RPC");
        assertNotEquals(FinancialMath.bd("3800"), quote.priceUsd());
    }
}