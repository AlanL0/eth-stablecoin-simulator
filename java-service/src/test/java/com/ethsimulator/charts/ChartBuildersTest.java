package com.ethsimulator.charts;

import com.ethsimulator.market.YieldQuote;
import com.ethsimulator.protocol.RateConvention;
import com.ethsimulator.simulation.RiskTier;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void treasuryContextChartBuildsEducationalBars() {
        var chart = ChartBuilders.treasuryContextChart(
                FinancialMath.bd("4222.22"),
                FinancialMath.bd("105.56"),
                FinancialMath.bd("4.91"),
                "maker_sky",
                FinancialMath.bd("4222.22"),
                FIXTURE_TIME
        );

        assertEquals("stablecoin_treasury_context", chart.chartId());
        assertEquals(3, chart.series().size());
        assertEquals("4222.22", chart.series().get(0).data().get(0).displayValue());
    }

    @Test
    void protocolRatesComparisonFlagsFallbackAndStaleQuotes() {
        Instant staleObserved = FIXTURE_TIME.minusSeconds(7200);
        var chart = ChartBuilders.protocolRatesComparison(
                "USDC",
                List.of(
                        new YieldQuote("aave", FinancialMath.bd("4.2"), RateConvention.APR_EFFECTIVE,
                                "seed", RiskTier.LOW, FIXTURE_TIME, false),
                        new YieldQuote("static_conservative", FinancialMath.bd("3.0"), RateConvention.APR_EFFECTIVE,
                                "static_fallback", RiskTier.LOW, staleObserved, true)
                ),
                FIXTURE_TIME
        );

        assertEquals(2, chart.series().get(0).data().size());
        assertEquals(2, chart.warnings().size());
        assertEquals("seed", chart.provenance().sources().get(0).source());
    }

    @Test
    void protocolRatesComparisonEmptyQuotesUseStaticFallbackProvenance() {
        var chart = ChartBuilders.protocolRatesComparison("UNKNOWN", List.of(), FIXTURE_TIME);

        assertTrue(chart.series().get(0).data().isEmpty());
        assertEquals("static_fallback", chart.provenance().sources().get(0).source());
    }

    @Test
    void yieldProjectionSpansMultipleYears() {
        var chart = ChartBuilders.yieldProjection(
                "maker_sky",
                FinancialMath.bd("4222.22"),
                FinancialMath.bd("211.11"),
                FinancialMath.bd("5.00"),
                3,
                12,
                FinancialMath.bd("3800"),
                FinancialMath.bd("2"),
                "chainlink",
                false,
                FIXTURE_TIME
        );

        assertEquals(3, chart.series().get(0).data().size());
        assertEquals(FinancialMath.bd("36"), chart.xAxis().domain().get(1).value());
    }

    @Test
    void ethPriceHistoryComputesDomainAndStaleWarning() {
        var chart = ChartBuilders.ethPriceHistory(
                List.of(
                        new ChartBuilders.EthHistoryPoint("2026-06-01T00:00:00Z", FinancialMath.bd("3000")),
                        new ChartBuilders.EthHistoryPoint("2026-06-02T00:00:00Z", FinancialMath.bd("4000.50"))
                ),
                "seed_history",
                true,
                FIXTURE_TIME
        );

        assertEquals(2, chart.series().get(0).data().size());
        assertEquals(FinancialMath.bd("3000.00"), chart.yAxis().domain().get(0).value());
        assertEquals(FinancialMath.bd("4000.50"), chart.yAxis().domain().get(1).value());
        assertFalse(chart.warnings().isEmpty());
    }

    @Test
    void ethPriceHistoryEmptyOmitsDomain() {
        var chart = ChartBuilders.ethPriceHistory(List.of(), "seed_history", false, FIXTURE_TIME);

        assertTrue(chart.series().get(0).data().isEmpty());
        assertEquals(null, chart.yAxis().domain());
    }

    @Test
    void yieldProjectionStaleEthPriceAddsWarning() {
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
                true,
                FIXTURE_TIME
        );

        assertEquals(1, chart.warnings().size());
        assertTrue(chart.provenance().sources().get(0).stale());
    }

    @Test
    void chartQueryParamsRoundTripToSimulationRequest() {
        ChartQueryParams params = new ChartQueryParams();
        params.setEthAmount(FinancialMath.bd("2"));
        params.setProtocol("maker_sky");
        params.setTargetCollateralRatio(FinancialMath.bd("1.8"));
        params.setLiquidationRatio(FinancialMath.bd("1.5"));
        params.setStabilityFeePct(FinancialMath.bd("5"));
        params.setDeployYieldPct(FinancialMath.bd("5"));
        params.setYears(2);
        params.setCompoundsPerYear(4);
        params.setEthPriceUsd(FinancialMath.bd("3800"));

        var request = params.toSimulationRequest();
        assertEquals(params.getEthAmount(), request.getEthAmount());
        assertEquals(params.getProtocol(), request.getProtocol());
        assertEquals(params.getYears(), request.getYears());
        assertEquals(Boolean.FALSE, request.getTreasuryContextEnabled());
    }
}