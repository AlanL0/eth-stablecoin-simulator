package com.ethsimulator.charts;

import com.ethsimulator.market.YieldQuote;
import com.ethsimulator.simulation.RiskTier;
import com.ethsimulator.util.FinancialMath;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** One-shot helper to refresh committed golden fixtures. */
class ChartFixtureGenerator {

    private static final Instant FIXTURE_TIME = Instant.parse("2026-06-09T12:00:01Z");
    private static final Path FIXTURE_DIR = Path.of("src/test/resources/fixtures/charts");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {
        write("simulation-yield-projection.json", ChartBuilders.yieldProjection(
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
        ));
        write("liquidation-price-band.json", ChartBuilders.liquidationBand(
                "maker_sky",
                FinancialMath.bd("3800"),
                FinancialMath.bd("3166.67"),
                FinancialMath.bd("2"),
                FinancialMath.bd("4222.22"),
                "chainlink",
                false,
                "ETH spot (Chainlink)",
                FIXTURE_TIME
        ));
        write("health-ratio-sweep.json", ChartBuilders.healthRatioSweep(
                "maker_sky",
                FinancialMath.bd("2"),
                FinancialMath.bd("4222.22"),
                FinancialMath.bd("1.50"),
                FinancialMath.bd("3800"),
                "chainlink",
                false,
                FIXTURE_TIME
        ));
        write("stablecoin-treasury-context.json", ChartBuilders.treasuryContextChart(
                FinancialMath.bd("3800.00"),
                FinancialMath.bd("171.00"),
                FinancialMath.bd("4.91"),
                "maker_sky",
                FinancialMath.bd("4222.22"),
                FIXTURE_TIME
        ));
        write("protocol-rates-comparison.json", ChartBuilders.protocolRatesComparison(
                "USDC",
                List.of(
                        new YieldQuote("aave", FinancialMath.bd("4.2"), "seed", RiskTier.LOW, FIXTURE_TIME),
                        new YieldQuote("compound", FinancialMath.bd("3.8"), "seed", RiskTier.LOW, FIXTURE_TIME),
                        new YieldQuote("maker_dsr", FinancialMath.bd("5.0"), "seed", RiskTier.MEDIUM, FIXTURE_TIME)
                ),
                FIXTURE_TIME
        ));
        write("eth-price-history.json", ChartBuilders.ethPriceHistory(
                new EthPriceHistoryService().seedHistory(),
                "seed_history",
                false,
                FIXTURE_TIME
        ));
    }

    private static void write(String name, ChartContract chart) throws Exception {
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(chart);
        Files.writeString(FIXTURE_DIR.resolve(name), json + System.lineSeparator(), StandardCharsets.UTF_8);
        System.out.println("Wrote " + name);
    }
}