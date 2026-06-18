package com.ethsimulator.charts;

import com.ethsimulator.simulation.SimulationEngine;
import com.ethsimulator.util.FinancialMath;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.ethsimulator.charts.ChartModels.*;

public final class ChartBuilders {

    private static final BigDecimal[] HEALTH_SWEEP_MULTIPLIERS = {
            FinancialMath.bd("0.5"),
            FinancialMath.bd("0.75"),
            FinancialMath.bd("1.0"),
            FinancialMath.bd("1.25"),
            FinancialMath.bd("1.5")
    };

    private ChartBuilders() {
    }

    public static ChartSpec yieldProjection(
            String protocol,
            BigDecimal stablecoinDebtUsd,
            BigDecimal annualStabilityFeeUsd,
            BigDecimal deployYieldPct,
            int years,
            int compoundsPerYear,
            BigDecimal ethPriceUsd,
            BigDecimal ethAmount,
            String ethPriceSource,
            boolean ethPriceStale,
            Instant generatedAt
    ) {
        int totalMonths = years * 12;
        List<Point> gross = new ArrayList<>();
        List<Point> fees = new ArrayList<>();
        List<Point> net = new ArrayList<>();

        BigDecimal ratePerPeriod = FinancialMath.divide(
                FinancialMath.humanPercentToRate(deployYieldPct),
                FinancialMath.bd(compoundsPerYear),
                FinancialMath.RATE_SCALE);

        for (int m = 0; m <= totalMonths; m += (totalMonths == 0 ? 1 : totalMonths / 2)) {
            if (m > totalMonths) {
                break;
            }
            int periods = FinancialMath.toIntRounded(FinancialMath.divide(
                    FinancialMath.multiply(FinancialMath.bd(m), FinancialMath.bd(compoundsPerYear)),
                    FinancialMath.bd(12),
                    0));
            BigDecimal growth = FinancialMath.add(BigDecimal.ONE, ratePerPeriod).pow(periods);
            BigDecimal grossUsd = FinancialMath.multiply(stablecoinDebtUsd, FinancialMath.subtract(growth, BigDecimal.ONE));
            BigDecimal feeUsd = FinancialMath.divide(
                    FinancialMath.multiply(annualStabilityFeeUsd, FinancialMath.bd(m)),
                    FinancialMath.bd(12),
                    FinancialMath.RATE_SCALE);
            BigDecimal netUsd = FinancialMath.subtract(grossUsd, feeUsd);
            gross.add(Point.ofXy(m, FinancialMath.scaleUsd(grossUsd)));
            fees.add(Point.ofXy(m, FinancialMath.scaleUsd(feeUsd)));
            net.add(Point.ofXy(m, FinancialMath.scaleUsd(netUsd)));
        }
        if (totalMonths > 0 && gross.stream().noneMatch(p -> ((Number) p.x()).intValue() == totalMonths)) {
            int m = totalMonths;
            int periods = years * compoundsPerYear;
            BigDecimal growth = FinancialMath.add(BigDecimal.ONE, ratePerPeriod).pow(periods);
            BigDecimal grossUsd = FinancialMath.multiply(stablecoinDebtUsd, FinancialMath.subtract(growth, BigDecimal.ONE));
            BigDecimal feeUsd = FinancialMath.multiply(annualStabilityFeeUsd, FinancialMath.bd(years));
            gross.add(Point.ofXy(m, FinancialMath.scaleUsd(grossUsd)));
            fees.add(Point.ofXy(m, FinancialMath.scaleUsd(feeUsd)));
            net.add(Point.ofXy(m, FinancialMath.scaleUsd(FinancialMath.subtract(grossUsd, feeUsd))));
        }

        String timestamp = generatedAt.toString();

        return new ChartSpec(
                "1.0",
                "simulation_yield_projection",
                "composed",
                "Projected Yield vs Fees",
                "Based on model assumptions — not live protocol guarantees",
                new Axis("linear", "Month", "month", "month_index",
                        List.of(FinancialMath.bd(0), FinancialMath.bd(totalMonths)), null),
                new Axis("linear", "Cumulative USD", "usd", "usd", null, null),
                List.of(
                        new Series("gross_yield", "Gross yield", "area", gross,
                                new SeriesStyle("positive", null, FinancialMath.bd("0.2"))),
                        new Series("cumulative_fees", "Cumulative stability fees", "line", fees,
                                new SeriesStyle("negative", "dashed", null)),
                        new Series("net_yield", "Net yield", "line", net,
                                new SeriesStyle("primary", null, null))
                ),
                List.of(),
                new Legend("bottom", true),
                meta(protocol, ethPriceUsd, ethAmount, FinancialMath.scaleUsd(stablecoinDebtUsd),
                        timestamp, ethPriceSource, ethPriceStale),
                "java-service/simulation-chart-builder",
                timestamp
        );
    }

    public static ChartSpec liquidationBand(
            String protocol,
            BigDecimal spotUsd,
            BigDecimal liquidationUsd,
            BigDecimal ethAmount,
            BigDecimal debtUsd,
            String ethPriceSource,
            boolean ethPriceStale,
            String spotLabel,
            Instant generatedAt
    ) {
        String timestamp = generatedAt.toString();

        return new ChartSpec(
                "1.0",
                "liquidation_price_band",
                "band",
                "ETH Spot vs Liquidation Price",
                null,
                new Axis("category", "Price marker", null, "usd", null, null),
                new Axis("linear", "USD per ETH", "usd", "usd", null, null),
                List.of(new Series("safe_band", "Collateral buffer", "band",
                        List.of(Point.band("range", liquidationUsd, FinancialMath.scaleUsd(spotUsd))),
                        new SeriesStyle("positive", null, FinancialMath.bd("0.15")))),
                List.of(
                        new Annotation("spot", "horizontal_line", "y", FinancialMath.scaleUsd(spotUsd), null, spotLabel, "info"),
                        new Annotation("liquidation", "horizontal_line", "y", liquidationUsd, null,
                                "Liquidation price", "high")
                ),
                null,
                meta(protocol, spotUsd, ethAmount, debtUsd, timestamp, ethPriceSource, ethPriceStale),
                "java-service/simulation-chart-builder",
                timestamp
        );
    }

    public static ChartSpec healthRatioSweep(
            String protocol,
            BigDecimal ethAmount,
            BigDecimal stablecoinDebtUsd,
            BigDecimal liquidationRatio,
            BigDecimal spotUsd,
            String ethPriceSource,
            boolean ethPriceStale,
            Instant generatedAt
    ) {
        List<Point> points = new ArrayList<>();
        BigDecimal minP = null;
        BigDecimal maxP = null;
        for (BigDecimal mult : HEALTH_SWEEP_MULTIPLIERS) {
            BigDecimal price = FinancialMath.multiply(spotUsd, mult);
            BigDecimal health = SimulationEngine.healthAtPrice(ethAmount, price, stablecoinDebtUsd, liquidationRatio);
            BigDecimal scaledPrice = FinancialMath.scaleUsd(price);
            BigDecimal scaledHealth = health.setScale(1, FinancialMath.INTERMEDIATE.getRoundingMode());
            points.add(Point.ofXy(scaledPrice, scaledHealth));
            minP = minP == null ? scaledPrice : FinancialMath.min(minP, scaledPrice);
            maxP = maxP == null ? scaledPrice : FinancialMath.max(maxP, scaledPrice);
        }

        String timestamp = generatedAt.toString();

        return new ChartSpec(
                "1.0",
                "health_ratio_sweep",
                "line",
                "Health Ratio Across ETH Prices",
                "Single-scenario sweep at current debt and protocol assumptions",
                new Axis("linear", "ETH price", "usd", "usd", List.of(minP, maxP), null),
                new Axis("linear", "Health ratio", null, "number",
                        List.of(FinancialMath.bd("0.5"), FinancialMath.bd("2.0")), null),
                List.of(new Series("health_ratio", "Health ratio", "line", points,
                        new SeriesStyle("primary", null, null))),
                List.of(
                        new Annotation("spot_marker", "vertical_line", "x", FinancialMath.scaleUsd(spotUsd), null,
                                "Current ETH price", "info"),
                        new Annotation("high_risk_band", "band", "y", FinancialMath.bd("0.5"),
                                FinancialMath.CHART_HIGH_RISK_UPPER,
                                "High risk zone", "high")
                ),
                null,
                meta(protocol, spotUsd, ethAmount, FinancialMath.scaleUsd(stablecoinDebtUsd),
                        timestamp, ethPriceSource, ethPriceStale),
                "java-service/simulation-chart-builder",
                timestamp
        );
    }

    public static ChartSpec treasuryContextChart(
            BigDecimal backingUsd,
            BigDecimal issuerYieldUsd,
            BigDecimal defiNetYieldUsd,
            String protocol,
            BigDecimal debtUsd,
            Instant generatedAt
    ) {
        String timestamp = generatedAt.toString();

        return new ChartSpec(
                "1.0",
                "stablecoin_treasury_context",
                "composed",
                "Stablecoin Reserves & Treasury Context (Educational)",
                "Your mint vs illustrative issuer reserve economics — not official fiscal data.",
                new Axis("category", "Your simulation (USD)", null, "usd", null, null),
                new Axis("linear", "USD", "usd", "usd", null, null),
                List.of(
                        new Series("treasury_backing", "Implied T-bill backing (your mint)", "bar",
                                List.of(Point.ofXy("your_mint", backingUsd)),
                                new SeriesStyle("secondary", null, null)),
                        new Series("issuer_yield", "Issuer reserve yield (annual)", "bar",
                                List.of(Point.ofXy("your_mint", issuerYieldUsd)),
                                new SeriesStyle("positive", null, null)),
                        new Series("your_defi_yield", "Your DeFi net yield (annual)", "bar",
                                List.of(Point.ofXy("your_mint", defiNetYieldUsd)),
                                new SeriesStyle("primary", null, null))
                ),
                List.of(),
                new Legend("bottom", true),
                new Meta(null, protocol, null, null, debtUsd,
                        List.of(new Source("tbillApyPct", "model_assumption", timestamp, false))),
                "java-service/treasury-context-builder",
                timestamp
        );
    }

    private static Meta meta(
            String protocol,
            BigDecimal ethPriceUsd,
            BigDecimal ethAmount,
            BigDecimal debtUsd,
            String observedAt,
            String ethPriceSource,
            boolean ethPriceStale
    ) {
        return new Meta(null, protocol, ethPriceUsd, ethAmount, debtUsd,
                List.of(new Source("ethPriceUsd", ethPriceSource, observedAt, ethPriceStale)));
    }
}