package com.ethsimulator.charts;

import com.ethsimulator.util.UsdMath;
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
                UsdMath.bd("4222.22"),
                UsdMath.bd("211.11"),
                UsdMath.bd("5.00"),
                1,
                12,
                3800,
                2,
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
                UsdMath.bd("4222.22"),
                UsdMath.bd("211.11"),
                UsdMath.bd("5.00"),
                1,
                12,
                3800,
                2,
                "chainlink",
                false,
                FIXTURE_TIME
        );

        var gross = chart.series().stream().filter(s -> "gross_yield".equals(s.id())).findFirst().orElseThrow();
        var fees = chart.series().stream().filter(s -> "cumulative_fees".equals(s.id())).findFirst().orElseThrow();
        var net = chart.series().stream().filter(s -> "net_yield".equals(s.id())).findFirst().orElseThrow();

        assertEquals(216.02, gross.points().get(2).y(), 0.02);
        assertEquals(211.11, fees.points().get(2).y(), 0.02);
        assertEquals(4.91, net.points().get(2).y(), 0.02);
    }

    @Test
    void liquidationBandStructure() {
        var chart = ChartBuilders.liquidationBand(
                "maker_sky", 3800, 3166.67, 2, 4222.22,
                "chainlink", false, "ETH spot (Chainlink)", FIXTURE_TIME);

        assertEquals("liquidation_price_band", chart.chartId());
        assertNotNull(chart.series().get(0).points().get(0).y0());
        assertEquals(3800.0, chart.series().get(0).points().get(0).y1(), 0.01);
    }

    @Test
    void healthSweepHasFivePointsAndAlignedRiskBand() {
        var chart = ChartBuilders.healthRatioSweep(
                "maker_sky",
                UsdMath.bd(2),
                UsdMath.bd("4222.22"),
                UsdMath.bd("1.50"),
                UsdMath.bd(3800),
                "chainlink",
                false,
                FIXTURE_TIME
        );

        assertEquals(5, chart.series().get(0).points().size());
        assertEquals(1.2, chart.series().get(0).points().get(2).y(), 0.05);
    }
}