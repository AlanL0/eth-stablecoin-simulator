package com.ethsimulator.util;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Deterministic financial arithmetic for all collateral, yield, and debt paths.
 * <p>
 * Rate conventions:
 * <ul>
 *   <li>{@link #simpleApr(BigDecimal, int)} — linear annualized rate (APR)</li>
 *   <li>{@link #effectiveApy(BigDecimal, int)} — compounded periodic rate (APY)</li>
 *   <li>{@link #trailingRealizedApr(BigDecimal, int, int)} — realized return annualized over elapsed periods</li>
 * </ul>
 */
public final class FinancialMath {

    public static final MathContext INTERMEDIATE = new MathContext(18, RoundingMode.HALF_UP);

    public static final int USD_SCALE = 2;
    public static final int ETH_SCALE = 4;
    public static final int RATE_SCALE = 8;
    public static final int RATIO_SCALE = 4;
    public static final int WAD_SCALE = 18;
    public static final int RAY_SCALE = 27;
    public static final int WEI_SCALE = 0;

    public static final BigDecimal ONE_HUNDRED = bd("100");
    public static final BigDecimal WAD = bd("1").movePointRight(WAD_SCALE);
    public static final BigDecimal RAY = bd("1").movePointRight(RAY_SCALE);

    public static final BigDecimal MAX_RATIO = bd("10");
    public static final BigDecimal MAX_PERCENT = bd("100");

    public static final BigDecimal LOW_HEALTH_THRESHOLD = bd("1.75");
    public static final BigDecimal MEDIUM_HEALTH_THRESHOLD = bd("1.25");
    public static final BigDecimal CHART_HIGH_RISK_UPPER = bd("1.25");

    private FinancialMath() {
    }

    public static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    public static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }

    public static BigDecimal bd(long value) {
        return BigDecimal.valueOf(value);
    }

    public static BigDecimal requireNonNull(BigDecimal value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    public static BigDecimal add(BigDecimal left, BigDecimal right) {
        return requireNonNull(left, "left").add(requireNonNull(right, "right"), INTERMEDIATE);
    }

    public static BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return requireNonNull(left, "left").subtract(requireNonNull(right, "right"), INTERMEDIATE);
    }

    public static BigDecimal multiply(BigDecimal left, BigDecimal right) {
        return requireNonNull(left, "left").multiply(requireNonNull(right, "right"), INTERMEDIATE);
    }

    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor, int resultScale) {
        requireNonNull(dividend, "dividend");
        BigDecimal normalizedDivisor = requireNonNull(divisor, "divisor");
        if (normalizedDivisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return dividend.divide(normalizedDivisor, resultScale, INTERMEDIATE.getRoundingMode());
    }

    public static BigDecimal scaleUsd(BigDecimal value) {
        return requireNonNull(value, "value").setScale(USD_SCALE, INTERMEDIATE.getRoundingMode());
    }

    public static BigDecimal scaleEth(BigDecimal value) {
        return requireNonNull(value, "value").setScale(ETH_SCALE, INTERMEDIATE.getRoundingMode());
    }

    public static BigDecimal scaleRate(BigDecimal value) {
        return requireNonNull(value, "value").setScale(RATE_SCALE, INTERMEDIATE.getRoundingMode());
    }

    public static BigDecimal scaleRatio(BigDecimal value) {
        return requireNonNull(value, "value").setScale(RATIO_SCALE, INTERMEDIATE.getRoundingMode());
    }

    public static BigDecimal scaleWad(BigDecimal value) {
        return requireNonNull(value, "value").setScale(WAD_SCALE, INTERMEDIATE.getRoundingMode());
    }

    public static BigDecimal scaleRay(BigDecimal value) {
        return requireNonNull(value, "value").setScale(RAY_SCALE, INTERMEDIATE.getRoundingMode());
    }

    public static BigDecimal scaleWei(BigDecimal value) {
        return requireNonNull(value, "value").setScale(WEI_SCALE, INTERMEDIATE.getRoundingMode());
    }

    /** Human percent (e.g. 5.00) to decimal rate (0.05). */
    public static BigDecimal humanPercentToRate(BigDecimal humanPercent) {
        return divide(requireNonNull(humanPercent, "humanPercent"), ONE_HUNDRED, RATE_SCALE);
    }

    /** Decimal rate to human percent. */
    public static BigDecimal rateToHumanPercent(BigDecimal rate) {
        return scaleUsd(multiply(requireNonNull(rate, "rate"), ONE_HUNDRED));
    }

    /** Simple APR: linear annualization (rate per period × periods per year). */
    public static BigDecimal simpleApr(BigDecimal ratePerPeriod, int periodsPerYear) {
        return multiply(requireNonNull(ratePerPeriod, "ratePerPeriod"), bd(periodsPerYear));
    }

    /** Effective APY from periodic compounding: (1 + r)^n − 1. */
    public static BigDecimal effectiveApy(BigDecimal ratePerPeriod, int periodsPerYear) {
        BigDecimal base = add(BigDecimal.ONE, requireNonNull(ratePerPeriod, "ratePerPeriod"));
        BigDecimal compounded = base.pow(periodsPerYear, INTERMEDIATE);
        return scaleRate(subtract(compounded, BigDecimal.ONE));
    }

    /** Trailing realized APR from cumulative return over elapsed periods. */
    public static BigDecimal trailingRealizedApr(BigDecimal cumulativeReturn, int elapsedPeriods, int periodsPerYear) {
        if (elapsedPeriods <= 0) {
            throw new IllegalArgumentException("elapsedPeriods must be positive");
        }
        BigDecimal perPeriod = divide(cumulativeReturn, bd(elapsedPeriods), RATE_SCALE);
        return multiply(perPeriod, bd(periodsPerYear));
    }

    public static BigDecimal wadFromDecimal(BigDecimal decimal) {
        return scaleWei(multiply(requireNonNull(decimal, "decimal"), WAD));
    }

    public static BigDecimal decimalFromWad(BigDecimal wad) {
        return scaleWad(divide(requireNonNull(wad, "wad"), WAD, WAD_SCALE));
    }

    public static BigDecimal rayFromDecimal(BigDecimal decimal) {
        return scaleWei(multiply(requireNonNull(decimal, "decimal"), RAY));
    }

    public static BigDecimal decimalFromRay(BigDecimal ray) {
        return scaleRay(divide(requireNonNull(ray, "ray"), RAY, RAY_SCALE));
    }

    public static BigDecimal parseJsonNumber(JsonNode node) {
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("JSON number node is required");
        }
        if (node.isBigDecimal()) {
            return node.decimalValue();
        }
        if (node.isNumber()) {
            return bd(node.asString().trim());
        }
        if (node.isString()) {
            return bd(node.asString().trim());
        }
        throw new IllegalArgumentException("JSON node is not a number: " + node);
    }

    public static int toIntRounded(BigDecimal value) {
        return requireNonNull(value, "value").setScale(0, INTERMEDIATE.getRoundingMode()).intValueExact();
    }

    public static BigDecimal min(BigDecimal left, BigDecimal right) {
        return requireNonNull(left, "left").min(requireNonNull(right, "right"));
    }

    public static BigDecimal max(BigDecimal left, BigDecimal right) {
        return requireNonNull(left, "left").max(requireNonNull(right, "right"));
    }
}