package com.ethsimulator.market;

import java.math.BigDecimal;
import java.time.Instant;

public record EthPriceQuote(
        BigDecimal priceUsd,
        EthPriceSource source,
        Instant observedAt,
        boolean stale
) {
    public EthPriceQuote withSource(EthPriceSource newSource) {
        return new EthPriceQuote(priceUsd, newSource, observedAt, stale);
    }
}