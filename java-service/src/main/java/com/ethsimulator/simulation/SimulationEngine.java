package com.ethsimulator.simulation;

import com.ethsimulator.util.FinancialMath;

import java.math.BigDecimal;

public final class SimulationEngine {

    private SimulationEngine() {
    }

    public record Result(
            BigDecimal collateralValueUsd,
            BigDecimal stablecoinDebtUsd,
            BigDecimal liquidationPriceUsd,
            BigDecimal annualStabilityFeeUsd,
            BigDecimal projectedGrossYieldUsd,
            BigDecimal projectedNetYieldUsd,
            BigDecimal healthRatio,
            RiskTier riskTier
    ) {
    }

    public static Result compute(
            BigDecimal ethAmount,
            BigDecimal ethPriceUsd,
            BigDecimal targetCollateralRatio,
            BigDecimal liquidationRatio,
            BigDecimal stabilityFeePct,
            BigDecimal deployYieldPct,
            int years,
            int compoundsPerYear
    ) {
        SimulationLimits.validateCompounding(years, compoundsPerYear);

        BigDecimal collateralValueUsd = FinancialMath.multiply(ethAmount, ethPriceUsd);
        BigDecimal stablecoinDebtUsd = FinancialMath.divide(
                collateralValueUsd, targetCollateralRatio, FinancialMath.RATE_SCALE);
        BigDecimal liquidationPriceUsd = FinancialMath.divide(
                FinancialMath.multiply(stablecoinDebtUsd, liquidationRatio),
                ethAmount,
                FinancialMath.RATE_SCALE);
        BigDecimal annualStabilityFeeUsd = FinancialMath.multiply(
                stablecoinDebtUsd, FinancialMath.humanPercentToRate(stabilityFeePct));

        BigDecimal ratePerPeriod = FinancialMath.divide(
                FinancialMath.humanPercentToRate(deployYieldPct),
                FinancialMath.bd(compoundsPerYear),
                FinancialMath.RATE_SCALE);
        BigDecimal growth = FinancialMath.add(BigDecimal.ONE, ratePerPeriod)
                .pow(years * compoundsPerYear, FinancialMath.INTERMEDIATE);
        BigDecimal projectedGrossYieldUsd = FinancialMath.multiply(
                stablecoinDebtUsd, FinancialMath.subtract(growth, BigDecimal.ONE));
        BigDecimal projectedNetYieldUsd = FinancialMath.subtract(
                projectedGrossYieldUsd,
                FinancialMath.multiply(annualStabilityFeeUsd, FinancialMath.bd(years)));

        BigDecimal healthRatio = FinancialMath.divide(
                collateralValueUsd,
                FinancialMath.multiply(stablecoinDebtUsd, liquidationRatio),
                FinancialMath.RATIO_SCALE);
        RiskTier riskTier = RiskTier.fromHealthRatio(healthRatio);

        return new Result(
                FinancialMath.scaleUsd(collateralValueUsd),
                FinancialMath.scaleUsd(stablecoinDebtUsd),
                FinancialMath.scaleUsd(liquidationPriceUsd),
                FinancialMath.scaleUsd(annualStabilityFeeUsd),
                FinancialMath.scaleUsd(projectedGrossYieldUsd),
                FinancialMath.scaleUsd(projectedNetYieldUsd),
                healthRatio,
                riskTier
        );
    }

    public static BigDecimal healthAtPrice(
            BigDecimal ethAmount,
            BigDecimal ethPriceUsd,
            BigDecimal stablecoinDebtUsd,
            BigDecimal liquidationRatio
    ) {
        BigDecimal collateral = FinancialMath.multiply(ethAmount, ethPriceUsd);
        return FinancialMath.divide(
                collateral,
                FinancialMath.multiply(stablecoinDebtUsd, liquidationRatio),
                FinancialMath.RATIO_SCALE);
    }
}