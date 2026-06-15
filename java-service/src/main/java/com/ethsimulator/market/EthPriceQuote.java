package com.ethsimulator.market;

import java.math.BigDecimal;
import java.time.Instant;

public record EthPriceQuote(
        BigDecimal priceUsd,
        EthPriceSource source,
        Instant observedAt,
        boolean stale,
        boolean degraded
) {
    public EthPriceQuote(BigDecimal priceUsd, EthPriceSource source, Instant observedAt, boolean stale) {
        this(priceUsd, source, observedAt, stale, source == EthPriceSource.STATIC);
    }

    public EthPriceQuote withSource(EthPriceSource newSource) {
        return new EthPriceQuote(priceUsd, newSource, observedAt, stale, degraded);
    }
}