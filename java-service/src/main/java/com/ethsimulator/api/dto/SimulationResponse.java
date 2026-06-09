package com.ethsimulator.api.dto;

import com.ethsimulator.charts.ChartModels.ChartSpec;
import com.ethsimulator.simulation.RiskTier;
import com.ethsimulator.treasury.TreasuryContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
        List<ChartSpec> charts
) {
    public record Assumptions(
            String protocol,
            double ethAmount,
            double ethPriceUsd,
            String ethPriceSource,
            double targetCollateralRatio,
            double liquidationRatio,
            double stabilityFeePct,
            double deployYieldPct,
            int years,
            int compoundsPerYear,
            String stabilityFeeModel
    ) {
    }
}