package com.ethsimulator.charts;

import com.ethsimulator.market.EthPriceQuote;
import com.ethsimulator.service.SimulationInputResolver.ResolvedSimulation;
import com.ethsimulator.util.FinancialMath;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class LiquidationBandChartBuilder {

    public ChartModels.ChartSpec build(ResolvedSimulation resolved, Instant generatedAt) {
        EthPriceQuote price = resolved.ethPrice();
        String spotLabel = spotLabel(price);
        return ChartBuilders.liquidationBand(
                resolved.protocol(),
                price.priceUsd(),
                resolved.result().liquidationPriceUsd(),
                FinancialMath.scaleEth(resolved.ethAmount()),
                resolved.result().stablecoinDebtUsd(),
                resolved.ethPriceSourceKey(),
                price.stale(),
                spotLabel,
                generatedAt
        );
    }

    private static String spotLabel(EthPriceQuote price) {
        return switch (price.source()) {
            case CHAINLINK -> "ETH spot (Chainlink)";
            case PUBLIC_API -> "ETH spot (public API)";
            case CACHE -> "ETH spot (cached)";
            case STATIC -> "ETH spot (static fallback)";
        };
    }
}