package com.ethsimulator.blockchain;

import java.math.BigDecimal;
import java.util.Optional;

public interface ChainlinkEthUsdReader {

    Optional<BigDecimal> readPriceUsd();
}