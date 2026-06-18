package com.ethsimulator.charts;

import com.ethsimulator.market.EthPriceQuote;
import com.ethsimulator.market.EthPriceSource;
import com.ethsimulator.simulation.SimulationEngine;
import com.ethsimulator.service.SimulationInputResolver.ResolvedSimulation;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiquidationBandChartBuilderTest {

    private final LiquidationBandChartBuilder builder = new LiquidationBandChartBuilder();

    @Test
    void usesJavaResolvedSpotNotClientHint() {
        EthPriceQuote resolvedPrice = new EthPriceQuote(
                FinancialMath.bd("3850"),
                EthPriceSource.CHAINLINK,
                Instant.parse("2026-06-09T12:00:00Z"),
                true
        );

        ResolvedSimulation resolved = new ResolvedSimulation(
                resolvedPrice,
                FinancialMath.bd("2"),
                SimulationEngine.compute(
                        FinancialMath.bd("2"),
                        resolvedPrice.priceUsd(),
                        FinancialMath.bd("1.80"),
                        FinancialMath.bd("1.50"),
                        FinancialMath.bd("5.00"),
                        FinancialMath.bd("5.00"),
                        1,
                        12
                ),
                "maker_sky",
                FinancialMath.bd("1.80"),
                FinancialMath.bd("1.50"),
                FinancialMath.bd("5.00"),
                FinancialMath.bd("5.00"),
                1,
                12
        );

        var chart = builder.build(resolved, Instant.parse("2026-06-09T12:00:01Z"));

        assertEquals("3850", chart.assumptions().get("ethPriceUsd"));
        assertEquals("chainlink", chart.provenance().sources().get(0).source());
        assertTrue(chart.provenance().sources().get(0).stale());
        assertEquals("ETH spot (Chainlink)", chart.annotations().get(0).label());
        assertEquals("3850.00", chart.series().get(0).data().get(0).metadata().get("displayValueEnd"));
    }
}