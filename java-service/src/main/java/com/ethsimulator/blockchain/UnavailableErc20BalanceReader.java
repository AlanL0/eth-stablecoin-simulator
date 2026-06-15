package com.ethsimulator.blockchain;

import com.ethsimulator.config.TokenAllowlist;

import java.math.BigDecimal;
import java.util.Optional;

public class UnavailableErc20BalanceReader implements Erc20BalanceReader {

    @Override
    public Optional<BigDecimal> readBalance(String walletAddress, TokenAllowlist.TokenEntry token) {
        return Optional.empty();
    }

    @Override
    public String source() {
        return "unavailable";
    }
}