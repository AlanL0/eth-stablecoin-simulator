package com.ethsimulator.simulation;

public enum RiskTier {
    LOW,
    MEDIUM,
    HIGH;

    public static RiskTier fromHealthRatio(double healthRatio) {
        if (healthRatio >= 1.5) {
            return LOW;
        }
        if (healthRatio >= 1.0) {
            return MEDIUM;
        }
        return HIGH;
    }
}