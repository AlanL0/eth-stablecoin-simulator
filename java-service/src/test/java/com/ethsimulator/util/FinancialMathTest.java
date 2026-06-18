package com.ethsimulator.util;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinancialMathTest {

    @Test
    void addSubtractMultiplyAtIntermediatePrecision() {
        BigDecimal a = FinancialMath.bd("0.1");
        BigDecimal b = FinancialMath.bd("0.2");
        assertEquals(FinancialMath.bd("0.3"), FinancialMath.add(a, b).stripTrailingZeros());
        assertEquals(FinancialMath.bd("0.3"), FinancialMath.multiply(FinancialMath.bd("3"), FinancialMath.bd("0.1")).stripTrailingZeros());
        assertEquals(FinancialMath.bd("0.7"), FinancialMath.subtract(FinancialMath.bd("1"), FinancialMath.bd("0.3")).stripTrailingZeros());
    }

    @Test
    void divideRequiresExplicitScaleAndRejectsZeroDivisor() {
        assertEquals(
                FinancialMath.bd("0.33"),
                FinancialMath.divide(FinancialMath.bd("1"), FinancialMath.bd("3"), 2)
        );
        assertThrows(ArithmeticException.class,
                () -> FinancialMath.divide(FinancialMath.bd("1"), BigDecimal.ZERO, 2));
    }

    @Test
    void handlesZeroNegativeVeryLargeAndVerySmallValues() {
        assertEquals(FinancialMath.bd("0.00"), FinancialMath.scaleUsd(BigDecimal.ZERO));
        assertEquals(FinancialMath.bd("-1.23"), FinancialMath.scaleUsd(FinancialMath.bd("-1.2345")));
        assertEquals(
                FinancialMath.bd("1000000000000000000.00"),
                FinancialMath.scaleUsd(FinancialMath.bd("999999999999999999.999"))
        );
        assertEquals(
                FinancialMath.bd("0.00"),
                FinancialMath.scaleUsd(FinancialMath.bd("0.000004"))
        );
    }

    @Test
    void humanPercentAndRateConversions() {
        assertEquals(
                FinancialMath.bd("0.05000000"),
                FinancialMath.humanPercentToRate(FinancialMath.bd("5"))
        );
        assertEquals(
                FinancialMath.bd("5.00"),
                FinancialMath.rateToHumanPercent(FinancialMath.bd("0.05"))
        );
    }

    @Test
    void simpleAprEffectiveApyAndTrailingRealizedApr() {
        BigDecimal ratePerPeriod = FinancialMath.humanPercentToRate(FinancialMath.bd("1"));
        assertEquals(
                FinancialMath.bd("0.12000000"),
                FinancialMath.simpleApr(ratePerPeriod, 12)
        );

        BigDecimal apy = FinancialMath.effectiveApy(ratePerPeriod, 12);
        assertEquals(FinancialMath.bd("0.12682503"), apy);

        BigDecimal cumulative = FinancialMath.bd("0.06");
        assertEquals(
                FinancialMath.bd("0.12000000"),
                FinancialMath.trailingRealizedApr(cumulative, 6, 12)
        );
    }

    @Test
    void wadAndRayConversionsRoundTrip() {
        BigDecimal decimal = FinancialMath.bd("1.5");
        BigDecimal wad = FinancialMath.wadFromDecimal(decimal);
        assertEquals(FinancialMath.bd("1500000000000000000"), wad);
        assertEquals(FinancialMath.bd("1.500000000000000000"), FinancialMath.decimalFromWad(wad));

        BigDecimal ray = FinancialMath.rayFromDecimal(decimal);
        assertEquals(FinancialMath.bd("1500000000000000000000000000"), ray);
        assertEquals(FinancialMath.bd("1.500000000000000000000000000"), FinancialMath.decimalFromRay(ray));
    }

    @Test
    void namedScalesMatchContract() {
        assertEquals(FinancialMath.bd("1234.57"), FinancialMath.scaleUsd(FinancialMath.bd("1234.5678")));
        assertEquals(FinancialMath.bd("1.2346"), FinancialMath.scaleEth(FinancialMath.bd("1.23456")));
        assertEquals(FinancialMath.bd("1.2346"), FinancialMath.scaleRatio(FinancialMath.bd("1.23456")));
        assertEquals(FinancialMath.bd("123456789012345679"), FinancialMath.scaleWei(FinancialMath.bd("123456789012345678.9")));
    }

    @Test
    void parseJsonNumberFromTextNeverUsesFloatingPoint() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode decimalNode = mapper.readTree("3925.5");
        JsonNode scientificNode = mapper.readTree("1.23e-4");

        assertEquals(FinancialMath.bd("3925.5"), FinancialMath.parseJsonNumber(decimalNode));
        assertEquals(FinancialMath.bd("0.000123"), FinancialMath.parseJsonNumber(scientificNode));
    }

    @Test
    void canonicalSimulationFixtureRemainsEquivalentAtUsdScale() {
        var result = com.ethsimulator.simulation.SimulationEngine.compute(
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
    }
}