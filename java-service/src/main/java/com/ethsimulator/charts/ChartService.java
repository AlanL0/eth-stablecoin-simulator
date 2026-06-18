package com.ethsimulator.charts;

import com.ethsimulator.service.SimulationInputResolver;
import com.ethsimulator.service.SimulationInputResolver.ResolvedSimulation;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class ChartService {

    private final SimulationInputResolver simulationInputResolver;
    private final LiquidationBandChartBuilder liquidationBandChartBuilder;
    private final Clock clock;

    public ChartService(
            SimulationInputResolver simulationInputResolver,
            LiquidationBandChartBuilder liquidationBandChartBuilder,
            Clock clock
    ) {
        this.simulationInputResolver = simulationInputResolver;
        this.liquidationBandChartBuilder = liquidationBandChartBuilder;
        this.clock = clock;
    }

    public ChartModels.ChartSpec simulationProjection(ChartQueryParams params) {
        ResolvedSimulation resolved = simulationInputResolver.resolve(params.toSimulationRequest());
        Instant generatedAt = clock.instant();
        return ChartBuilders.yieldProjection(
                resolved.protocol(),
                resolved.result().stablecoinDebtUsd(),
                resolved.result().annualStabilityFeeUsd(),
                resolved.deployYieldPct(),
                resolved.years(),
                resolved.compoundsPerYear(),
                resolved.ethPrice().priceUsd(),
                resolved.scaledEthAmount(),
                resolved.ethPriceSourceKey(),
                resolved.ethPrice().stale(),
                generatedAt
        );
    }

    public ChartModels.ChartSpec liquidationBand(ChartQueryParams params) {
        ResolvedSimulation resolved = simulationInputResolver.resolve(params.toSimulationRequest());
        return liquidationBandChartBuilder.build(resolved, clock.instant());
    }

    public ChartModels.ChartSpec healthRatioSweep(ChartQueryParams params) {
        ResolvedSimulation resolved = simulationInputResolver.resolve(params.toSimulationRequest());
        Instant generatedAt = clock.instant();
        return ChartBuilders.healthRatioSweep(
                resolved.protocol(),
                resolved.ethAmount(),
                resolved.result().stablecoinDebtUsd(),
                resolved.liquidationRatio(),
                resolved.ethPrice().priceUsd(),
                resolved.ethPriceSourceKey(),
                resolved.ethPrice().stale(),
                generatedAt
        );
    }
}