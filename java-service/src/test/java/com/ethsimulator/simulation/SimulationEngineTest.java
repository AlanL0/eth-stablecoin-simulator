package com.ethsimulator.simulation;

import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationEngineTest {

    @Test
    void makerPresetCanonicalFixture() {
        SimulationEngine.Result result = SimulationEngine.compute(
                FinancialMath.bd("2"),
                FinancialMath.bd("3800"),
                FinancialMath.bd("1.80"),
                FinancialMath.bd("1.50"),
                FinancialMath.bd("5.00"),
                FinancialMath.bd("5.00"),
                1,
                12
        );

        assertEquals(FinancialMath.bd("7600.00"), result.collateralValueUsd());
        assertEquals(FinancialMath.bd("4222.22"), result.stablecoinDebtUsd());
        assertEquals(FinancialMath.bd("3166.67"), result.liquidationPriceUsd());
        assertEquals(FinancialMath.bd("211.11"), result.annualStabilityFeeUsd());
        assertEquals(FinancialMath.bd("216.02"), result.projectedGrossYieldUsd());
        assertEquals(FinancialMath.bd("4.91"), result.projectedNetYieldUsd());
        assertEquals(FinancialMath.bd("1.2000"), result.healthRatio());
        assertEquals(RiskTier.HIGH, result.riskTier());
    }

    @Test
    void healthAtSpotMatchesResult() {
        BigDecimal ethAmount = FinancialMath.bd("2");
        BigDecimal ethPrice = FinancialMath.bd("3800");
        BigDecimal debt = FinancialMath.bd("4222.22");
        BigDecimal liquidationRatio = FinancialMath.bd("1.50");

        BigDecimal health = SimulationEngine.healthAtPrice(ethAmount, ethPrice, debt, liquidationRatio);
        assertEquals(FinancialMath.bd("1.2000"), health);
    }
}