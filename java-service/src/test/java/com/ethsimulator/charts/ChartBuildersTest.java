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
        assertEquals(3, chart.series().get(0).data().size());
        assertEquals(FIXTURE_TIME.toString(), chart.provenance().generatedAt());
        assertEquals("static", chart.provenance().sources().get(0).source());
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

        assertEquals("216.02", gross.data().get(2).displayValue());
        assertEquals("211.11", fees.data().get(2).displayValue());
        assertEquals("4.91", net.data().get(2).displayValue());
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
        assertNotNull(chart.series().get(0).data().get(0).metadata().get("displayValueEnd"));
        assertEquals(FinancialMath.scaleUsd(FinancialMath.bd("3800")).toPlainString(),
                chart.series().get(0).data().get(0).metadata().get("displayValueEnd"));
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

        assertEquals(5, chart.series().get(0).data().size());
        assertEquals("1.2", chart.series().get(0).data().get(2).displayValue());
    }
}