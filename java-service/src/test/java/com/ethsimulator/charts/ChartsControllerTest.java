package com.ethsimulator.charts;

import com.ethsimulator.blockchain.ChainlinkEthUsdReader;
import com.ethsimulator.market.EthPriceCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.Matchers.closeTo;
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

    @MockBean
    private ChainlinkEthUsdReader chainlinkEthUsdReader;

    @BeforeEach
    void setUp() {
        ethPriceCache.clear();
        when(chainlinkEthUsdReader.readPriceUsd()).thenReturn(Optional.of(new BigDecimal("3850")));
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
                .andExpect(jsonPath("$.chartId", is("simulation_yield_projection")))
                .andExpect(jsonPath("$.meta.sources[0].source", is("chainlink")));
    }

    @Test
    void liquidationBandUsesResolvedPriceNotClientOverride() throws Exception {
        mockMvc.perform(get("/api/charts/liquidation-band")
                        .param("ethAmount", "2")
                        .param("protocol", "maker_sky")
                        .param("ethPriceUsd", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chartId", is("liquidation_price_band")))
                .andExpect(jsonPath("$.meta.ethPriceUsd", closeTo(3850.0, 0.01)))
                .andExpect(jsonPath("$.meta.sources[0].source", is("chainlink")))
                .andExpect(jsonPath("$.meta.sources[0].stale", is(true)))
                .andExpect(jsonPath("$.annotations[0].label", is("ETH spot (Chainlink)")));
    }

    @Test
    void healthRatioEndpoint() throws Exception {
        mockMvc.perform(get("/api/charts/health-ratio")
                        .param("ethAmount", "2")
                        .param("protocol", "maker_sky"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chartId", is("health_ratio_sweep")))
                .andExpect(jsonPath("$.series[0].points[2].y", closeTo(1.2, 0.05)));
    }
}