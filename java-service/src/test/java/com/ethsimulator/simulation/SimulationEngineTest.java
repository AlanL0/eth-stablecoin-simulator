package com.ethsimulator.simulation;

import com.ethsimulator.util.UsdMath;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationEngineTest {

    @Test
    void makerPresetCanonicalFixture() {
        SimulationEngine.Result result = SimulationEngine.compute(
                UsdMath.bd(2),
                UsdMath.bd(3800),
                UsdMath.bd("1.80"),
                UsdMath.bd("1.50"),
                UsdMath.bd("5.00"),
                UsdMath.bd("5.00"),
                1,
                12
        );

        assertEquals(UsdMath.bd("7600.00"), result.collateralValueUsd());
        assertEquals(UsdMath.bd("4222.22"), result.stablecoinDebtUsd());
        assertEquals(UsdMath.bd("3166.67"), result.liquidationPriceUsd());
        assertEquals(UsdMath.bd("211.11"), result.annualStabilityFeeUsd());
        assertEquals(UsdMath.bd("216.02"), result.projectedGrossYieldUsd());
        assertEquals(UsdMath.bd("4.91"), result.projectedNetYieldUsd());
        assertEquals(UsdMath.bd("1.2000"), result.healthRatio());
        assertEquals(RiskTier.MEDIUM, result.riskTier());
    }

    @Test
    void healthAtSpotMatchesResult() {
        BigDecimal ethAmount = UsdMath.bd(2);
        BigDecimal ethPrice = UsdMath.bd(3800);
        BigDecimal debt = UsdMath.bd("4222.22");
        BigDecimal liquidationRatio = UsdMath.bd("1.50");

        double health = SimulationEngine.healthAtPrice(ethAmount, ethPrice, debt, liquidationRatio);
        assertEquals(1.2, health, 0.05);
    }
}