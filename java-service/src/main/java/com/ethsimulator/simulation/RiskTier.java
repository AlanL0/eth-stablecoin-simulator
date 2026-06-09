package com.ethsimulator.simulation;

public enum RiskTier {
    LOW,
    MEDIUM,
    HIGH;

    public static final double LOW_THRESHOLD = 1.75;
    public static final double MEDIUM_THRESHOLD = 1.25;
    public static final double CHART_HIGH_RISK_UPPER = 1.25;

    public static RiskTier fromHealthRatio(double healthRatio) {
        if (healthRatio >= LOW_THRESHOLD) {
            return LOW;
        }
        if (healthRatio >= MEDIUM_THRESHOLD) {
            return MEDIUM;
        }
        return HIGH;
    }
}