package com.ethsimulator.wallet;

import com.ethsimulator.blockchain.Erc20BalanceReader;
import com.ethsimulator.config.TokenAllowlist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ethsimulator.util.FinancialMath;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final String ADDRESS = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-15T12:00:00Z");

    @Mock
    private Erc20BalanceReader balanceReader;

    @Test
    void mapsBalancesWithOneDollarPegAssumption() {
        when(balanceReader.source()).thenReturn("chain");
        when(balanceReader.readBalance(eq(ADDRESS), any(TokenAllowlist.TokenEntry.class)))
                .thenReturn(Optional.of(BigDecimal.ZERO));
        when(balanceReader.readBalance(eq(ADDRESS), eq(TokenAllowlist.bySymbol("USDT").orElseThrow())))
                .thenReturn(Optional.of(new BigDecimal("100.25")));

        WalletService service = new WalletService(balanceReader, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        WalletStablecoinsResponse response = service.stablecoinBalances(ADDRESS);

        assertThat(response.address()).isEqualTo(ADDRESS);
        assertThat(response.observedAt()).isEqualTo(FIXED_INSTANT.toString());
        assertThat(response.balances()).hasSize(4);
        assertThat(response.balances().get(1).symbol()).isEqualTo("USDT");
        assertThat(response.balances().get(1).balance()).isEqualTo("100.25");
        assertThat(response.balances().get(1).balanceUsd()).isEqualTo(FinancialMath.bd("100.25"));
        assertThat(response.assumptions()).anyMatch(assumption -> assumption.contains("$1.00 USD"));
    }
}