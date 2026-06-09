package com.ethsimulator.simulation;

import com.ethsimulator.util.UsdMath;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
        BigDecimal collateralValueUsd = ethAmount.multiply(ethPriceUsd);
        BigDecimal stablecoinDebtUsd = collateralValueUsd.divide(targetCollateralRatio, 10, RoundingMode.HALF_UP);
        BigDecimal liquidationPriceUsd = stablecoinDebtUsd.multiply(liquidationRatio)
                .divide(ethAmount, 10, RoundingMode.HALF_UP);
        BigDecimal annualStabilityFeeUsd = stablecoinDebtUsd.multiply(UsdMath.percentToRate(stabilityFeePct));

        BigDecimal ratePerPeriod = UsdMath.percentToRate(deployYieldPct)
                .divide(BigDecimal.valueOf(compoundsPerYear), 10, RoundingMode.HALF_UP);
        BigDecimal growth = BigDecimal.ONE.add(ratePerPeriod)
                .pow(years * compoundsPerYear);
        BigDecimal projectedGrossYieldUsd = stablecoinDebtUsd.multiply(growth.subtract(BigDecimal.ONE));
        BigDecimal projectedNetYieldUsd = projectedGrossYieldUsd.subtract(
                annualStabilityFeeUsd.multiply(BigDecimal.valueOf(years)));

        BigDecimal healthRatio = collateralValueUsd.divide(
                stablecoinDebtUsd.multiply(liquidationRatio), 10, RoundingMode.HALF_UP);
        RiskTier riskTier = RiskTier.fromHealthRatio(healthRatio.doubleValue());

        return new Result(
                UsdMath.roundUsd(collateralValueUsd),
                UsdMath.roundUsd(stablecoinDebtUsd),
                UsdMath.roundUsd(liquidationPriceUsd),
                UsdMath.roundUsd(annualStabilityFeeUsd),
                UsdMath.roundUsd(projectedGrossYieldUsd),
                UsdMath.roundUsd(projectedNetYieldUsd),
                healthRatio.setScale(4, RoundingMode.HALF_UP),
                riskTier
        );
    }

    public static double healthAtPrice(BigDecimal ethAmount, BigDecimal ethPriceUsd,
                                       BigDecimal stablecoinDebtUsd, BigDecimal liquidationRatio) {
        BigDecimal collateral = ethAmount.multiply(ethPriceUsd);
        return collateral.divide(stablecoinDebtUsd.multiply(liquidationRatio), 10, RoundingMode.HALF_UP)
                .doubleValue();
    }
}