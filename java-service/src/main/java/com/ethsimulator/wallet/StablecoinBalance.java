package com.ethsimulator.wallet;

public record StablecoinBalance(
        String symbol,
        String contractAddress,
        int decimals,
        String balance,
        double balanceUsd
) {
}