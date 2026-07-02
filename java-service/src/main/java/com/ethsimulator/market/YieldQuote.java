package com.ethsimulator.market;

import com.ethsimulator.protocol.RateConvention;
import com.ethsimulator.simulation.RiskTier;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record YieldQuote(
        String protocol,
        @Schema(description = "Annualized yield as human percent", example = "4.20")
        BigDecimal apyPct,
        @Schema(description = "Explicit annualization convention for apyPct")
        RateConvention convention,
        String source,
        RiskTier riskTier,
        @Schema(description = "True observation time for live rows; EPOCH for static seed assumptions")
        Instant observedAt,
        @Schema(description = "True when data is stale, seed-based, or otherwise not live-ingested")
        boolean degraded
) {
}