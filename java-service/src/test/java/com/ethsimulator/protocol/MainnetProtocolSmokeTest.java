package com.ethsimulator.protocol;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ETH_RPC_URL", matches = ".+")
class MainnetProtocolSmokeTest {

    @Autowired(required = false)
    private List<ProtocolAdapter> adapters;

    @Test
    void liveMainnetAdaptersReturnProvenance() {
        Assumptions.assumeTrue(adapters != null && !adapters.isEmpty(), "Web3j adapters require ETH_RPC_URL");

        boolean anyQuote = false;
        for (ProtocolAdapter adapter : adapters) {
            if (!adapter.enabled()) {
                continue;
            }
            List<ProtocolRateQuote> quotes = adapter.fetchQuotes();
            for (ProtocolRateQuote quote : quotes) {
                anyQuote = true;
                assertNotNull(quote.protocol());
                assertNotNull(quote.product());
                assertNotNull(quote.provenance());
                assertTrue(quote.provenance().blockNumber() > 0);
                assertTrue(StringUtils.hasText(quote.provenance().blockHash()));
                assertTrue(
                        quote.rateOptional().isPresent() || quote.unavailableReasonOptional().isPresent(),
                        "adapter must not silently substitute static values"
                );
            }
        }
        assertTrue(anyQuote, "expected at least one live protocol quote");
        assertFalse(adapters.isEmpty());
    }
}