package com.ethsimulator.charts;

import com.ethsimulator.market.YieldQuote;
import com.ethsimulator.simulation.SimulationEngine;
import com.ethsimulator.util.FinancialMath;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ethsimulator.charts.ChartContract.*;
import static com.ethsimulator.charts.ChartPoints.*;

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

    public static ChartContract yieldProjection(
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
        List<DataPoint> gross = new ArrayList<>();
        List<DataPoint> fees = new ArrayList<>();
        List<DataPoint> net = new ArrayList<>();

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
            gross.add(xyUsd(m, grossUsd));
            fees.add(xyUsd(m, feeUsd));
            net.add(xyUsd(m, netUsd));
        }
        if (totalMonths > 0 && gross.stream().noneMatch(p -> ((Number) p.x()).intValue() == totalMonths)) {
            int m = totalMonths;
            int periods = years * compoundsPerYear;
            BigDecimal growth = FinancialMath.add(BigDecimal.ONE, ratePerPeriod).pow(periods);
            BigDecimal grossUsd = FinancialMath.multiply(stablecoinDebtUsd, FinancialMath.subtract(growth, BigDecimal.ONE));
            BigDecimal feeUsd = FinancialMath.multiply(annualStabilityFeeUsd, FinancialMath.bd(years));
            gross.add(xyUsd(m, grossUsd));
            fees.add(xyUsd(m, feeUsd));
            net.add(xyUsd(m, FinancialMath.subtract(grossUsd, feeUsd)));
        }

        String timestamp = generatedAt.toString();
        List<String> warnings = ethPriceStale
                ? List.of("ETH spot price is stale; chart uses last observed quote.")
                : List.of();

        return new ChartContract(
                SCHEMA_VERSION,
                "simulation_yield_projection",
                "Gross/Net Protocol Return vs Stability Fees",
                "Based on model assumptions — not live protocol guarantees",
                new ChartAxis("linear", "Month", "month", "month_index",
                        List.of(PlotNumber.of(FinancialMath.bd(0)), PlotNumber.of(FinancialMath.bd(totalMonths))), null),
                new ChartAxis("linear", "Cumulative USD", "usd", "usd", null, null),
                List.of(
                        new ChartSeries("gross_yield", "Gross protocol return (annualized)", "usd",
                                new ChartSeriesStyle("area", "positive", null, FinancialMath.bd("0.2")),
                                gross),
                        new ChartSeries("cumulative_fees", "Cumulative stability fees", "usd",
                                new ChartSeriesStyle("line", "negative", "dashed", null),
                                fees),
                        new ChartSeries("net_yield", "Net protocol return (annualized)", "usd",
                                new ChartSeriesStyle("line", "primary", null, null),
                                net)
                ),
                List.of(),
                assumptions(protocol, ethPriceUsd, ethAmount, stablecoinDebtUsd, "composed"),
                warnings,
                provenance("java-service/simulation-chart-builder", timestamp, ethPriceSource, ethPriceStale,
                        "linear_annualized_v1")
        );
    }

    public static ChartContract liquidationBand(
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
        List<String> warnings = ethPriceStale
                ? List.of("ETH spot price is stale; collateral recovery threshold uses last observed quote.")
                : List.of();

        return new ChartContract(
                SCHEMA_VERSION,
                "liquidation_price_band",
                "ETH Spot vs Collateral Recovery Threshold",
                null,
                new ChartAxis("category", "Price marker", null, "usd", null, null),
                new ChartAxis("linear", "USD per ETH", "usd", "usd", null, null),
                List.of(new ChartSeries("safe_band", "Collateral buffer", "usd",
                        new ChartSeriesStyle("band", "positive", null, FinancialMath.bd("0.15")),
                        List.of(band("range", liquidationUsd, FinancialMath.scaleUsd(spotUsd))))),
                List.of(
                        annotation("spot", "horizontal_line", "y", FinancialMath.scaleUsd(spotUsd),
                                FinancialMath.USD_SCALE, spotLabel, "info"),
                        annotation("liquidation", "horizontal_line", "y", liquidationUsd,
                                FinancialMath.USD_SCALE, "Collateral recovery threshold", "high")
                ),
                assumptions(protocol, spotUsd, ethAmount, debtUsd, "band"),
                warnings,
                provenance("java-service/simulation-chart-builder", timestamp, ethPriceSource, ethPriceStale, null)
        );
    }

    public static ChartContract healthRatioSweep(
            String protocol,
            BigDecimal ethAmount,
            BigDecimal stablecoinDebtUsd,
            BigDecimal liquidationRatio,
            BigDecimal spotUsd,
            String ethPriceSource,
            boolean ethPriceStale,
            Instant generatedAt
    ) {
        List<DataPoint> points = new ArrayList<>();
        BigDecimal minP = null;
        BigDecimal maxP = null;
        for (BigDecimal mult : HEALTH_SWEEP_MULTIPLIERS) {
            BigDecimal price = FinancialMath.multiply(spotUsd, mult);
            BigDecimal health = SimulationEngine.healthAtPrice(ethAmount, price, stablecoinDebtUsd, liquidationRatio);
            BigDecimal scaledPrice = FinancialMath.scaleUsd(price);
            BigDecimal scaledHealth = health.setScale(1, FinancialMath.INTERMEDIATE.getRoundingMode());
            points.add(xy(scaledPrice, scaledHealth, 1));
            minP = minP == null ? scaledPrice : FinancialMath.min(minP, scaledPrice);
            maxP = maxP == null ? scaledPrice : FinancialMath.max(maxP, scaledPrice);
        }

        String timestamp = generatedAt.toString();
        List<String> warnings = ethPriceStale
                ? List.of("ETH spot price is stale; collateralization sweep uses last observed quote.")
                : List.of();

        return new ChartContract(
                SCHEMA_VERSION,
                "health_ratio_sweep",
                "Collateralization Risk Margin Across ETH Prices",
                "Single-scenario sweep at current debt and protocol assumptions",
                new ChartAxis("linear", "ETH price", "usd", "usd",
                        List.of(PlotNumber.of(minP), PlotNumber.of(maxP)), null),
                new ChartAxis("linear", "Collateralization risk margin", null, "number",
                        List.of(PlotNumber.of(FinancialMath.bd("0.5")), PlotNumber.of(FinancialMath.bd("2.0"))), null),
                List.of(new ChartSeries("collateralization_risk_margin", "Collateralization risk margin", "ratio",
                        new ChartSeriesStyle("line", "primary", null, null),
                        points)),
                List.of(
                        annotation("spot_marker", "vertical_line", "x", FinancialMath.scaleUsd(spotUsd),
                                FinancialMath.USD_SCALE, "Current ETH price", "info"),
                        annotationBand("high_risk_band", "y",
                                FinancialMath.bd("0.5"),
                                FinancialMath.CHART_HIGH_RISK_UPPER,
                                2,
                                "High risk zone",
                                "high")
                ),
                assumptions(protocol, spotUsd, ethAmount, stablecoinDebtUsd, "line"),
                warnings,
                provenance("java-service/simulation-chart-builder", timestamp, ethPriceSource, ethPriceStale, null)
        );
    }

    public static ChartContract treasuryContextChart(
            BigDecimal backingUsd,
            BigDecimal issuerYieldUsd,
            BigDecimal defiNetYieldUsd,
            String protocol,
            BigDecimal debtUsd,
            Instant generatedAt
    ) {
        String timestamp = generatedAt.toString();

        return new ChartContract(
                SCHEMA_VERSION,
                "stablecoin_treasury_context",
                "Stablecoin Reserves & Treasury Context (Educational)",
                "Your mint vs illustrative issuer reserve economics — not official fiscal data.",
                new ChartAxis("category", "Your simulation (USD)", null, "usd", null, null),
                new ChartAxis("linear", "USD", "usd", "usd", null, null),
                List.of(
                        new ChartSeries("treasury_backing", "Implied T-bill backing (your mint)", "usd",
                                new ChartSeriesStyle("bar", "secondary", null, null),
                                List.of(xyUsd("your_mint", backingUsd))),
                        new ChartSeries("issuer_yield", "Issuer reserve yield (annual)", "usd",
                                new ChartSeriesStyle("bar", "positive", null, null),
                                List.of(xyUsd("your_mint", issuerYieldUsd))),
                        new ChartSeries("your_defi_yield", "Deployed capital net return (annualized)", "usd",
                                new ChartSeriesStyle("bar", "primary", null, null),
                                List.of(xyUsd("your_mint", defiNetYieldUsd)))
                ),
                List.of(),
                assumptions(protocol, null, null, debtUsd, "composed"),
                List.of(),
                new ChartProvenance(
                        "java-service/treasury-context-builder",
                        timestamp,
                        "treasury_reserve_model_v1",
                        List.of(new ChartSource("tbillApyPct", "model_assumption", timestamp, false))
                )
        );
    }

    public static ChartContract protocolRatesComparison(
            String asset,
            List<YieldQuote> quotes,
            Instant generatedAt
    ) {
        String timestamp = generatedAt.toString();
        List<DataPoint> points = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        boolean seedWarningAdded = false;
        boolean staleWarningAdded = false;
        for (YieldQuote quote : quotes) {
            points.add(xy(quote.protocol(), quote.apyPct(), FinancialMath.USD_SCALE));
            if (!seedWarningAdded
                    && (quote.degraded()
                    || "static_fallback".equals(quote.source())
                    || "seed".equals(quote.source()))) {
                warnings.add("Protocol return quotes use static fallback seed data.");
                seedWarningAdded = true;
            }
            if (!staleWarningAdded
                    && quote.observedAt() != null
                    && quote.observedAt().isBefore(generatedAt.minusSeconds(3600))) {
                warnings.add("One or more protocol return quotes may be stale.");
                staleWarningAdded = true;
            }
        }

        return new ChartContract(
                SCHEMA_VERSION,
                "protocol_rates_comparison",
                "Protocol Return Comparison (Annualized)",
                "Illustrative deployed-capital net returns by venue — not investment advice.",
                new ChartAxis("category", "Protocol venue", null, "string", null, null),
                new ChartAxis("linear", "Protocol return (annualized)", "percent", "percent", null, null),
                List.of(new ChartSeries("protocol_return", "Protocol return (annualized)", "percent",
                        new ChartSeriesStyle("bar", "primary", null, null),
                        points)),
                List.of(),
                Map.of("asset", asset, "chartType", "bar"),
                warnings,
                new ChartProvenance(
                        "java-service/protocol-rates-chart-builder",
                        timestamp,
                        "seed_catalog_v1",
                        List.of(new ChartSource("apyPct", quotes.isEmpty() ? "static_fallback" : quotes.get(0).source(),
                                timestamp, false))
                )
        );
    }

    public static ChartContract ethPriceHistory(
            List<EthHistoryPoint> history,
            String priceSource,
            boolean stale,
            Instant generatedAt
    ) {
        String timestamp = generatedAt.toString();
        List<DataPoint> points = new ArrayList<>();
        BigDecimal min = null;
        BigDecimal max = null;

        for (EthHistoryPoint point : history) {
            BigDecimal plot = FinancialMath.scaleUsd(point.priceUsd());
            points.add(xyUsdPreserveExact(point.observedAt(), point.priceUsd()));
            min = min == null ? plot : FinancialMath.min(min, plot);
            max = max == null ? plot : FinancialMath.max(max, plot);
        }

        List<String> warnings = stale
                ? List.of("ETH price history includes stale terminal observation.")
                : List.of();

        return new ChartContract(
                SCHEMA_VERSION,
                "eth_price_history",
                "ETH/USD Price History",
                "Deterministic seed history for educational context — not a live market data feed.",
                new ChartAxis("time", "Observation time", null, "iso8601", null, null),
                new ChartAxis("linear", "USD per ETH", "usd", "usd",
                        min != null && max != null ? List.of(PlotNumber.of(min), PlotNumber.of(max)) : null, null),
                List.of(new ChartSeries("eth_spot", "ETH spot (USD)", "usd",
                        new ChartSeriesStyle("line", "primary", null, null),
                        points)),
                List.of(),
                Map.of("chartType", "line"),
                warnings,
                provenance("java-service/eth-price-history-builder", timestamp, priceSource, stale, "seed_history_v1")
        );
    }

    public record EthHistoryPoint(String observedAt, BigDecimal priceUsd) {
    }

    private static Map<String, String> assumptions(
            String protocol,
            BigDecimal ethPriceUsd,
            BigDecimal ethAmount,
            BigDecimal debtUsd,
            String chartType
    ) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("chartType", chartType);
        if (protocol != null) {
            map.put("protocol", protocol);
        }
        if (ethPriceUsd != null) {
            map.put("ethPriceUsd", ethPriceUsd.toPlainString());
        }
        if (ethAmount != null) {
            map.put("ethAmount", ethAmount.toPlainString());
        }
        if (debtUsd != null) {
            map.put("stablecoinDebtUsd", debtUsd.toPlainString());
        }
        return map;
    }

    private static ChartProvenance provenance(
            String builder,
            String generatedAt,
            String ethPriceSource,
            boolean ethPriceStale,
            String methodology
    ) {
        return new ChartProvenance(
                builder,
                generatedAt,
                methodology,
                List.of(new ChartSource("ethPriceUsd", ethPriceSource, generatedAt, ethPriceStale))
        );
    }
}