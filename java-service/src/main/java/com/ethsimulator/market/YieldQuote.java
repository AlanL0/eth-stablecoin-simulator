package com.ethsimulator.market;

import com.ethsimulator.simulation.RiskTier;

import java.math.BigDecimal;
import java.time.Instant;

public record YieldQuote(
        String protocol,
        BigDecimal apyPct,
        String source,
        RiskTier riskTier,
        Instant observedAt
) {
}