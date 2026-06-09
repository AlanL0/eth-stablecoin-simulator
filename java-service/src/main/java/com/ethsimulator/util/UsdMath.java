package com.ethsimulator.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class UsdMath {

    private static final int SCALE = 10;
    private static final int USD_SCALE = 2;

    private UsdMath() {
    }

    public static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }

    public static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    public static BigDecimal percentToRate(BigDecimal humanPercent) {
        return humanPercent.divide(bd(100), SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal roundUsd(BigDecimal value) {
        return value.setScale(USD_SCALE, RoundingMode.HALF_UP);
    }

    public static double roundUsdDouble(BigDecimal value) {
        return roundUsd(value).doubleValue();
    }
}