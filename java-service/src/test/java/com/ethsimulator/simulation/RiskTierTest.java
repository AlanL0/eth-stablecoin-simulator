package com.ethsimulator.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskTierTest {

    @Test
    void canonicalHealthRatioIsHighAndInsideChartBand() {
        assertEquals(RiskTier.HIGH, RiskTier.fromHealthRatio(1.2));
        assertEquals(RiskTier.HIGH, RiskTier.fromHealthRatio(1.24));
    }

    @Test
    void mediumBandStartsAtChartHighRiskUpperBound() {
        assertEquals(RiskTier.MEDIUM, RiskTier.fromHealthRatio(1.25));
        assertEquals(RiskTier.MEDIUM, RiskTier.fromHealthRatio(1.5));
    }

    @Test
    void lowBandStartsAtPlanThreshold() {
        assertEquals(RiskTier.LOW, RiskTier.fromHealthRatio(1.75));
        assertEquals(RiskTier.LOW, RiskTier.fromHealthRatio(2.0));
    }
}