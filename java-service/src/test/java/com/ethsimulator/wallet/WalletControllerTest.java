package com.ethsimulator.wallet;

import com.ethsimulator.blockchain.Erc20BalanceReader;
import com.ethsimulator.config.TokenAllowlist;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletControllerTest {

    private static final String VALID_ADDRESS = "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Erc20BalanceReader erc20BalanceReader;

    @BeforeEach
    void setUp() {
        when(erc20BalanceReader.source()).thenReturn("chain");
        when(erc20BalanceReader.readBalance(eq(VALID_ADDRESS.toLowerCase()), any(TokenAllowlist.TokenEntry.class)))
                .thenReturn(Optional.empty());
        when(erc20BalanceReader.readBalance(
                eq(VALID_ADDRESS.toLowerCase()),
                eq(TokenAllowlist.bySymbol("USDC").orElseThrow())
        )).thenReturn(Optional.of(new BigDecimal("1250.50")));
        when(erc20BalanceReader.readBalance(
                eq(VALID_ADDRESS.toLowerCase()),
                eq(TokenAllowlist.bySymbol("DAI").orElseThrow())
        )).thenReturn(Optional.of(new BigDecimal("42")));
    }

    @Test
    void returnsAllowlistedStablecoinBalances() throws Exception {
        mockMvc.perform(get("/api/wallet/{address}/stablecoins", VALID_ADDRESS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address", is(VALID_ADDRESS.toLowerCase())))
                .andExpect(jsonPath("$.balances", hasSize(4)))
                .andExpect(jsonPath("$.balances[0].symbol", is("USDC")))
                .andExpect(jsonPath("$.balances[0].balance", is("1250.5")))
                .andExpect(jsonPath("$.balances[0].balanceUsd", closeTo(1250.5, 0.01)))
                .andExpect(jsonPath("$.balances[0].decimals", is(6)))
                .andExpect(jsonPath("$.balances[2].symbol", is("DAI")))
                .andExpect(jsonPath("$.balances[2].balance", is("42")))
                .andExpect(jsonPath("$.balances[2].balanceUsd", closeTo(42.0, 0.01)))
                .andExpect(jsonPath("$.source", is("chain")))
                .andExpect(jsonPath("$.observedAt").exists())
                .andExpect(jsonPath("$.assumptions", hasSize(3)));
    }

    @Test
    void rejectsInvalidAddress() throws Exception {
        mockMvc.perform(get("/api/wallet/{address}/stablecoins", "not-an-address"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_ADDRESS")));
    }
}