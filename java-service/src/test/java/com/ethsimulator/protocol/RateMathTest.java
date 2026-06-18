package com.ethsimulator.protocol;

import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateMathTest {

    @Test
    void convertsBasisPointsWadRayAndTokenDecimals() {
        assertEquals(
                FinancialMath.bd("0.05000000"),
                RateMath.bpsToDecimalRate(500)
        );
        assertEquals(
                FinancialMath.bd("5.00"),
                RateMath.bpsToHumanPercent(500)
        );
        assertEquals(
                FinancialMath.bd("0.05000000"),
                RateMath.wadRatioToDecimalRate(new BigInteger("50000000000000000"))
        );
        assertEquals(
                FinancialMath.bd("0.05000000"),
                RateMath.rayAnnualToDecimalRate(new BigInteger("50000000000000000000000000"))
        );
        assertEquals(
                FinancialMath.bd("3500.00"),
                RateMath.chainlinkAnswerToUsd(new BigInteger("350000000000"), 8)
        );
        assertEquals(
                FinancialMath.bd("12.50000000"),
                RateMath.tokenAmount(new BigInteger("12500000000000000000"), 18)
        );
    }

    @Test
    void annualizesMakerRayMultipliersAndTrailingRealizedApr() {
        BigInteger perSecond = FinancialMath.RAY.toBigInteger().add(new BigInteger("1000000000000000000"));
        assertEquals(
                FinancialMath.bd("0.03153600"),
                RateMath.makerRayPerSecondMultiplierToSimpleApr(perSecond)
        );

        BigInteger annualRay = new BigInteger("50000000000000000000000000");
        assertEquals(
                FinancialMath.bd("0.05000000"),
                RateMath.rayAnnualToDecimalRate(annualRay)
        );

        assertEquals(
                FinancialMath.bd("0.12166545"),
                RateMath.trailingRealizedApr(FinancialMath.bd("0.01"), 30, 365)
        );
    }
}