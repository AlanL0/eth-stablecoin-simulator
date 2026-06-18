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
        Instant observedAt
) {
}