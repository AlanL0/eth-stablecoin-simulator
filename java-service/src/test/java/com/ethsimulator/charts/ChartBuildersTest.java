package com.ethsimulator.charts;

import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChartBuildersTest {

    private static final Instant FIXTURE_TIME = Instant.parse("2026-06-09T12:00:01Z");

    @Test
    void yieldProjectionHasExpectedSeriesLengthForOneYear() {
        var chart = ChartBuilders.yieldProjection(
                "maker_sky",
                FinancialMath.bd("4222.22"),
                FinancialMath.bd("211.11"),
                FinancialMath.bd("5.00"),
                1,
                12,
                FinancialMath.bd("3800"),
                FinancialMath.bd("2"),
                "static",
                false,
                FIXTURE_TIME
        );

        assertEquals("simulation_yield_projection", chart.chartId());
        assertEquals(3, chart.series().get(0).points().size());
        assertEquals(FIXTURE_TIME.toString(), chart.generatedAt());
        assertEquals("static", chart.meta().sources().get(0).source());
    }

    @Test
    void yieldProjectionEndpointsMatchFixtureMath() {
        var chart = ChartBuilders.yieldProjection(
                "maker_sky",
                FinancialMath.bd("4222.22"),
                FinancialMath.bd("211.11"),
                FinancialMath.bd("5.00"),
                1,
                12,
                FinancialMath.bd("3800"),
                FinancialMath.bd("2"),
                "chainlink",
                false,
                FIXTURE_TIME
        );

        var gross = chart.series().stream().filter(s -> "gross_yield".equals(s.id())).findFirst().orElseThrow();
        var fees = chart.series().stream().filter(s -> "cumulative_fees".equals(s.id())).findFirst().orElseThrow();
        var net = chart.series().stream().filter(s -> "net_yield".equals(s.id())).findFirst().orElseThrow();

        assertEquals(FinancialMath.bd("216.02"), gross.points().get(2).y());
        assertEquals(FinancialMath.bd("211.11"), fees.points().get(2).y());
        assertEquals(FinancialMath.bd("4.91"), net.points().get(2).y());
    }

    @Test
    void liquidationBandStructure() {
        var chart = ChartBuilders.liquidationBand(
                "maker_sky",
                FinancialMath.bd("3800"),
                FinancialMath.bd("3166.67"),
                FinancialMath.bd("2"),
                FinancialMath.bd("4222.22"),
                "chainlink",
                false,
                "ETH spot (Chainlink)",
                FIXTURE_TIME);

        assertEquals("liquidation_price_band", chart.chartId());
        assertNotNull(chart.series().get(0).points().get(0).y0());
        assertEquals(FinancialMath.bd("3800.00"), chart.series().get(0).points().get(0).y1());
    }

    @Test
    void healthSweepHasFivePointsAndAlignedRiskBand() {
        var chart = ChartBuilders.healthRatioSweep(
                "maker_sky",
                FinancialMath.bd("2"),
                FinancialMath.bd("4222.22"),
                FinancialMath.bd("1.50"),
                FinancialMath.bd("3800"),
                "chainlink",
                false,
                FIXTURE_TIME
        );

        assertEquals(5, chart.series().get(0).points().size());
        assertEquals(FinancialMath.bd("1.2"), chart.series().get(0).points().get(2).y());
    }
}