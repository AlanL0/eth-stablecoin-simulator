package com.ethsimulator.charts;

import com.ethsimulator.market.EthPriceQuote;
import com.ethsimulator.market.EthPriceSource;
import com.ethsimulator.simulation.SimulationEngine;
import com.ethsimulator.service.SimulationInputResolver.ResolvedSimulation;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiquidationBandChartBuilderTest {

    private final LiquidationBandChartBuilder builder = new LiquidationBandChartBuilder();

    @ParameterizedTest
    @EnumSource(EthPriceSource.class)
    void spotLabelReflectsResolvedSource(EthPriceSource source) {
        EthPriceQuote resolvedPrice = new EthPriceQuote(
                FinancialMath.bd("3850"),
                source,
                Instant.parse("2026-06-09T12:00:00Z"),
                false
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

        String expectedLabel = switch (source) {
            case CHAINLINK -> "ETH spot (Chainlink)";
            case PUBLIC_API -> "ETH spot (public API)";
            case CACHE -> "ETH spot (cached)";
            case STATIC -> "ETH spot (static fallback)";
        };
        assertEquals(expectedLabel, chart.annotations().get(0).label());
    }
}