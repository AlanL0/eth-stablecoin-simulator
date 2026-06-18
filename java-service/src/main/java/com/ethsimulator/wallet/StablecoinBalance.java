package com.ethsimulator.wallet;

import java.math.BigDecimal;

public record StablecoinBalance(
        String symbol,
        String contractAddress,
        int decimals,
        String balance,
        BigDecimal balanceUsd
) {
}