package com.ethsimulator.charts;

import com.ethsimulator.blockchain.ChainlinkEthUsdReader;
import com.ethsimulator.market.EthPriceCache;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChartsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EthPriceCache ethPriceCache;

    @MockitoBean
    private ChainlinkEthUsdReader chainlinkEthUsdReader;

    @BeforeEach
    void setUp() {
        ethPriceCache.clear();
        when(chainlinkEthUsdReader.readPriceUsd()).thenReturn(Optional.of(FinancialMath.bd("3850")));
    }

    @Test
    void simulationProjectionEndpoint() throws Exception {
        mockMvc.perform(get("/api/charts/simulation-projection")
                        .param("ethAmount", "2")
                        .param("protocol", "maker_sky")
                        .param("deployYieldPct", "5")
                        .param("years", "1")
                        .param("compoundsPerYear", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion", is("2.0")))
                .andExpect(jsonPath("$.chartId", is("simulation_yield_projection")))
                .andExpect(jsonPath("$.provenance.sources[0].source", is("chainlink")));
    }

    @Test
    void liquidationBandUsesResolvedPriceNotClientOverride() throws Exception {
        mockMvc.perform(get("/api/charts/liquidation-band")
                        .param("ethAmount", "2")
                        .param("protocol", "maker_sky")
                        .param("ethPriceUsd", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion", is("2.0")))
                .andExpect(jsonPath("$.chartId", is("liquidation_price_band")))
                .andExpect(jsonPath("$.assumptions.ethPriceUsd", is("3850")))
                .andExpect(jsonPath("$.provenance.sources[0].source", is("chainlink")))
                .andExpect(jsonPath("$.provenance.sources[0].stale", is(true)))
                .andExpect(jsonPath("$.annotations[0].label", is("ETH spot (Chainlink)")));
    }

    @Test
    void healthRatioEndpoint() throws Exception {
        mockMvc.perform(get("/api/charts/health-ratio")
                        .param("ethAmount", "2")
                        .param("protocol", "maker_sky"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion", is("2.0")))
                .andExpect(jsonPath("$.chartId", is("health_ratio_sweep")))
                .andExpect(jsonPath("$.series[0].data[2].displayValue", is("1.2")));
    }

    @Test
    void protocolRatesEndpoint() throws Exception {
        mockMvc.perform(get("/api/charts/protocol-rates").param("asset", "USDC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion", is("2.0")))
                .andExpect(jsonPath("$.chartId", is("protocol_rates_comparison")));
    }

    @Test
    void ethPriceHistoryEndpoint() throws Exception {
        mockMvc.perform(get("/api/charts/eth-price-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion", is("2.0")))
                .andExpect(jsonPath("$.chartId", is("eth_price_history")))
                .andExpect(jsonPath("$.series[0].data[0].displayValue", is("3521.445678901234567890")));
    }
}