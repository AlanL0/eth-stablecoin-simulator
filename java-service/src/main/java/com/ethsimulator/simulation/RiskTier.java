package com.ethsimulator.simulation;

import com.ethsimulator.util.FinancialMath;

import java.math.BigDecimal;

public enum RiskTier {
    LOW,
    MEDIUM,
    HIGH;

    public static RiskTier fromHealthRatio(BigDecimal healthRatio) {
        if (healthRatio.compareTo(FinancialMath.LOW_HEALTH_THRESHOLD) >= 0) {
            return LOW;
        }
        if (healthRatio.compareTo(FinancialMath.MEDIUM_HEALTH_THRESHOLD) >= 0) {
            return MEDIUM;
        }
        return HIGH;
    }
}