package com.ethsimulator.blockchain;

import java.math.BigDecimal;
import java.util.Optional;

public class UnavailableChainlinkEthUsdReader implements ChainlinkEthUsdReader {

    @Override
    public Optional<BigDecimal> readPriceUsd() {
        return Optional.empty();
    }
}