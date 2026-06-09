package com.ethsimulator.charts;

import com.ethsimulator.simulation.RiskTier;
import com.ethsimulator.simulation.SimulationEngine;
import com.ethsimulator.util.UsdMath;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.ethsimulator.charts.ChartModels.*;

public final class ChartBuilders {

    private ChartBuilders() {
    }

    public static ChartSpec yieldProjection(
            String protocol,
            BigDecimal stablecoinDebtUsd,
            BigDecimal annualStabilityFeeUsd,
            BigDecimal deployYieldPct,
            int years,
            int compoundsPerYear,
            double ethPriceUsd,
            double ethAmount,
            String ethPriceSource,
            boolean ethPriceStale,
            Instant generatedAt
    ) {
        int totalMonths = years * 12;
        List<Point> gross = new ArrayList<>();
        List<Point> fees = new ArrayList<>();
        List<Point> net = new ArrayList<>();

        BigDecimal ratePerPeriod = UsdMath.percentToRate(deployYieldPct)
                .divide(BigDecimal.valueOf(compoundsPerYear), 10, RoundingMode.HALF_UP);

        for (int m = 0; m <= totalMonths; m += (totalMonths == 0 ? 1 : totalMonths / 2)) {
            if (m > totalMonths) {
                break;
            }
            int periods = (int) Math.round((double) m / 12.0 * compoundsPerYear);
            BigDecimal growth = BigDecimal.ONE.add(ratePerPeriod).pow(periods);
            BigDecimal grossUsd = stablecoinDebtUsd.multiply(growth.subtract(BigDecimal.ONE));
            BigDecimal feeUsd = annualStabilityFeeUsd.multiply(BigDecimal.valueOf(m))
                    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
            BigDecimal netUsd = grossUsd.subtract(feeUsd);
            gross.add(Point.ofXy(m, UsdMath.roundUsdDouble(grossUsd)));
            fees.add(Point.ofXy(m, UsdMath.roundUsdDouble(feeUsd)));
            net.add(Point.ofXy(m, UsdMath.roundUsdDouble(netUsd)));
        }
        if (totalMonths > 0 && gross.stream().noneMatch(p -> ((Number) p.x()).intValue() == totalMonths)) {
            int m = totalMonths;
            int periods = years * compoundsPerYear;
            BigDecimal growth = BigDecimal.ONE.add(ratePerPeriod).pow(periods);
            BigDecimal grossUsd = stablecoinDebtUsd.multiply(growth.subtract(BigDecimal.ONE));
            BigDecimal feeUsd = annualStabilityFeeUsd.multiply(BigDecimal.valueOf(years));
            gross.add(Point.ofXy(m, UsdMath.roundUsdDouble(grossUsd)));
            fees.add(Point.ofXy(m, UsdMath.roundUsdDouble(feeUsd)));
            net.add(Point.ofXy(m, UsdMath.roundUsdDouble(grossUsd.subtract(feeUsd))));
        }

        String timestamp = generatedAt.toString();

        return new ChartSpec(
                "1.0",
                "simulation_yield_projection",
                "composed",
                "Projected Yield vs Fees",
                "Based on model assumptions — not live protocol guarantees",
                new Axis("linear", "Month", "month", "month_index", List.of(0.0, (double) totalMonths), null),
                new Axis("linear", "Cumulative USD", "usd", "usd", null, null),
                List.of(
                        new Series("gross_yield", "Gross yield", "area", gross,
                                new SeriesStyle("positive", null, 0.2)),
                        new Series("cumulative_fees", "Cumulative stability fees", "line", fees,
                                new SeriesStyle("negative", "dashed", null)),
                        new Series("net_yield", "Net yield", "line", net,
                                new SeriesStyle("primary", null, null))
                ),
                List.of(),
                new Legend("bottom", true),
                meta(protocol, ethPriceUsd, ethAmount, UsdMath.roundUsdDouble(stablecoinDebtUsd),
                        timestamp, ethPriceSource, ethPriceStale),
                "java-service/simulation-chart-builder",
                timestamp
        );
    }

    public static ChartSpec liquidationBand(
            String protocol,
            double spotUsd,
            double liquidationUsd,
            double ethAmount,
            double debtUsd,
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
                        List.of(Point.band("range", liquidationUsd, spotUsd)),
                        new SeriesStyle("positive", null, 0.15))),
                List.of(
                        new Annotation("spot", "horizontal_line", "y", spotUsd, null, spotLabel, "info"),
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
        double[] multipliers = {0.5, 0.75, 1.0, 1.25, 1.5};
        List<Point> points = new ArrayList<>();
        double minP = Double.MAX_VALUE;
        double maxP = Double.MIN_VALUE;
        for (double mult : multipliers) {
            BigDecimal price = spotUsd.multiply(BigDecimal.valueOf(mult));
            double health = SimulationEngine.healthAtPrice(ethAmount, price, stablecoinDebtUsd, liquidationRatio);
            double p = UsdMath.roundUsdDouble(price);
            points.add(Point.ofXy(p, Math.round(health * 10.0) / 10.0));
            minP = Math.min(minP, p);
            maxP = Math.max(maxP, p);
        }

        String timestamp = generatedAt.toString();

        return new ChartSpec(
                "1.0",
                "health_ratio_sweep",
                "line",
                "Health Ratio Across ETH Prices",
                "Single-scenario sweep at current debt and protocol assumptions",
                new Axis("linear", "ETH price", "usd", "usd", List.of(minP, maxP), null),
                new Axis("linear", "Health ratio", null, "number", List.of(0.5, 2.0), null),
                List.of(new Series("health_ratio", "Health ratio", "line", points,
                        new SeriesStyle("primary", null, null))),
                List.of(
                        new Annotation("spot_marker", "vertical_line", "x", UsdMath.roundUsdDouble(spotUsd), null,
                                "Current ETH price", "info"),
                        new Annotation("high_risk_band", "band", "y", 0.5, RiskTier.CHART_HIGH_RISK_UPPER,
                                "High risk zone", "high")
                ),
                null,
                meta(protocol, spotUsd.doubleValue(), ethAmount.doubleValue(),
                        UsdMath.roundUsdDouble(stablecoinDebtUsd), timestamp, ethPriceSource, ethPriceStale),
                "java-service/simulation-chart-builder",
                timestamp
        );
    }

    public static ChartSpec treasuryContextChart(
            double backingUsd,
            double issuerYieldUsd,
            double defiNetYieldUsd,
            String protocol,
            double debtUsd,
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
            double ethPriceUsd,
            double ethAmount,
            double debtUsd,
            String observedAt,
            String ethPriceSource,
            boolean ethPriceStale
    ) {
        return new Meta(null, protocol, ethPriceUsd, ethAmount, debtUsd,
                List.of(new Source("ethPriceUsd", ethPriceSource, observedAt, ethPriceStale)));
    }
}