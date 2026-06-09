package com.ethsimulator.charts;

import com.ethsimulator.market.EthPriceQuote;
import com.ethsimulator.market.EthPriceSource;
import com.ethsimulator.simulation.SimulationEngine;
import com.ethsimulator.service.SimulationInputResolver.ResolvedSimulation;
import com.ethsimulator.util.UsdMath;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiquidationBandChartBuilderTest {

    private final LiquidationBandChartBuilder builder = new LiquidationBandChartBuilder();

    @Test
    void usesJavaResolvedSpotNotClientHint() {
        EthPriceQuote resolvedPrice = new EthPriceQuote(
                new BigDecimal("3850"),
                EthPriceSource.CHAINLINK,
                Instant.parse("2026-06-09T12:00:00Z"),
                true
        );

        ResolvedSimulation resolved = new ResolvedSimulation(
                resolvedPrice,
                UsdMath.bd(2),
                SimulationEngine.compute(
                        UsdMath.bd(2),
                        resolvedPrice.priceUsd(),
                        UsdMath.bd("1.80"),
                        UsdMath.bd("1.50"),
                        UsdMath.bd("5.00"),
                        UsdMath.bd("5.00"),
                        1,
                        12
                ),
                "maker_sky",
                UsdMath.bd("1.80"),
                UsdMath.bd("1.50"),
                UsdMath.bd("5.00"),
                UsdMath.bd("5.00"),
                1,
                12
        );

        var chart = builder.build(resolved, Instant.parse("2026-06-09T12:00:01Z"));

        assertEquals(3850.0, chart.meta().ethPriceUsd(), 0.01);
        assertEquals("chainlink", chart.meta().sources().get(0).source());
        assertTrue(chart.meta().sources().get(0).stale());
        assertEquals("ETH spot (Chainlink)", chart.annotations().get(0).label());
        assertEquals(3850.0, chart.series().get(0).points().get(0).y1(), 0.01);
    }
}