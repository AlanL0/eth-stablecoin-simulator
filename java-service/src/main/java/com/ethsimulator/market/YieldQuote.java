package com.ethsimulator.market;

import com.ethsimulator.simulation.RiskTier;

import java.time.Instant;

public record YieldQuote(
        String protocol,
        double apyPct,
        String source,
        RiskTier riskTier,
        Instant observedAt
) {
}