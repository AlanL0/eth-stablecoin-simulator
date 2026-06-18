package com.ethsimulator.api.dto;

import com.ethsimulator.charts.ChartContract;
import io.swagger.v3.oas.annotations.media.Schema;
import com.ethsimulator.simulation.RiskTier;
import com.ethsimulator.treasury.TreasuryContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(name = "SimulationResponse")
public record SimulationResponse(
        UUID id,
        BigDecimal collateralValueUsd,
        BigDecimal stablecoinDebtUsd,
        BigDecimal liquidationPriceUsd,
        BigDecimal annualStabilityFeeUsd,
        BigDecimal projectedGrossYieldUsd,
        BigDecimal projectedNetYieldUsd,
        BigDecimal healthRatio,
        RiskTier riskTier,
        List<String> warnings,
        Assumptions assumptions,
        TreasuryContext treasuryContext,
        @Schema(description = "ChartContract v2 payloads")
        List<ChartContract> charts
) {
    public record Assumptions(
            String protocol,
            BigDecimal ethAmount,
            BigDecimal ethPriceUsd,
            String ethPriceSource,
            BigDecimal targetCollateralRatio,
            BigDecimal liquidationRatio,
            BigDecimal stabilityFeePct,
            BigDecimal deployYieldPct,
            int years,
            int compoundsPerYear,
            String stabilityFeeModel
    ) {
    }
}