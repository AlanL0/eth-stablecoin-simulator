package com.ethsimulator.api;

import com.ethsimulator.blockchain.ChainlinkEthUsdReader;
import com.ethsimulator.market.EthPriceCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
class EthPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EthPriceCache ethPriceCache;

    @MockitoBean
    private ChainlinkEthUsdReader chainlinkEthUsdReader;

    @BeforeEach
    void setUp() {
        ethPriceCache.clear();
        when(chainlinkEthUsdReader.readPriceUsd()).thenReturn(Optional.of(new BigDecimal("3850")));
    }

    @Test
    void returnsChainlinkPriceWithSourceMetadata() throws Exception {
        mockMvc.perform(get("/api/price/eth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceUsd", closeTo(3850.0, 0.01)))
                .andExpect(jsonPath("$.source", is("chainlink")))
                .andExpect(jsonPath("$.observedAt").exists())
                .andExpect(jsonPath("$.stale", is(false)))
                .andExpect(jsonPath("$.degraded", is(false)));
    }
}