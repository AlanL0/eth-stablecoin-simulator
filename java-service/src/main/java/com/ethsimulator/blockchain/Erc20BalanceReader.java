package com.ethsimulator.blockchain;

import com.ethsimulator.config.TokenAllowlist;

import java.math.BigDecimal;
import java.util.Optional;

public interface Erc20BalanceReader {

    Optional<BigDecimal> readBalance(String walletAddress, TokenAllowlist.TokenEntry token);

    String source();
}