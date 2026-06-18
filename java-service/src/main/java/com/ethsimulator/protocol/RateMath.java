package com.ethsimulator.protocol;

import com.ethsimulator.util.FinancialMath;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * Unit conversions for protocol adapter boundaries: WAD/RAY, basis points, APR/APY, token decimals.
 */
public final class RateMath {

    public static final int SECONDS_PER_YEAR = 365 * 24 * 60 * 60;
    public static final int SECONDS_PER_DAY = 24 * 60 * 60;
    public static final MathContext COMPOUND = new MathContext(18, FinancialMath.INTERMEDIATE.getRoundingMode());

    private RateMath() {
    }

    public static BigDecimal tokenAmount(BigInteger raw, int decimals) {
        return FinancialMath.scaleRate(new BigDecimal(raw).movePointLeft(decimals));
    }

    public static BigDecimal bpsToDecimalRate(int basisPoints) {
        return FinancialMath.divide(FinancialMath.bd(basisPoints), FinancialMath.bd(10_000), FinancialMath.RATE_SCALE);
    }

    public static BigDecimal bpsToHumanPercent(int basisPoints) {
        return FinancialMath.scaleUsd(FinancialMath.divide(FinancialMath.bd(basisPoints), FinancialMath.bd(100), 2));
    }

    public static BigDecimal wadRatioToDecimalRate(BigInteger wadRatio) {
        return FinancialMath.scaleRate(FinancialMath.decimalFromWad(new BigDecimal(wadRatio)));
    }

    public static BigDecimal rayAnnualToDecimalRate(BigInteger annualRay) {
        return FinancialMath.scaleRate(FinancialMath.decimalFromRay(new BigDecimal(annualRay)));
    }

    public static BigDecimal makerRayPerSecondMultiplierToSimpleApr(BigInteger perSecondMultiplierRay) {
        BigDecimal multiplier = FinancialMath.divide(new BigDecimal(perSecondMultiplierRay), FinancialMath.RAY, FinancialMath.RAY_SCALE);
        BigDecimal ratePerSecond = FinancialMath.subtract(multiplier, BigDecimal.ONE);
        return FinancialMath.scaleRate(FinancialMath.simpleApr(ratePerSecond, SECONDS_PER_YEAR));
    }

    public static BigDecimal makerRayPerSecondMultiplierToEffectiveApr(BigInteger perSecondMultiplierRay) {
        BigDecimal multiplier = FinancialMath.divide(new BigDecimal(perSecondMultiplierRay), FinancialMath.RAY, FinancialMath.RAY_SCALE);
        if (multiplier.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // Compound in day-sized chunks to avoid overflowing BigDecimal.pow(secondsPerYear).
        BigDecimal dailyMultiplier = multiplier.pow(SECONDS_PER_DAY, COMPOUND);
        BigDecimal annualMultiplier = dailyMultiplier.pow(365, COMPOUND);
        return FinancialMath.scaleRate(FinancialMath.subtract(annualMultiplier, BigDecimal.ONE));
    }

    public static BigDecimal trailingRealizedApr(
            BigDecimal cumulativeReturn,
            int elapsedDays,
            int periodsPerYear
    ) {
        return FinancialMath.trailingRealizedApr(cumulativeReturn, elapsedDays, periodsPerYear);
    }

    public static BigDecimal chainlinkAnswerToUsd(BigInteger answer, int decimals) {
        if (answer == null || answer.signum() <= 0) {
            throw new IllegalArgumentException("Chainlink answer must be positive");
        }
        return FinancialMath.scaleUsd(new BigDecimal(answer).movePointLeft(decimals));
    }
}