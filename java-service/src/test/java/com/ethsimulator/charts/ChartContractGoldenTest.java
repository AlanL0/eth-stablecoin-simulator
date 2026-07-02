package com.ethsimulator.charts;

import com.ethsimulator.market.YieldQuote;
import com.ethsimulator.simulation.RiskTier;
import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartContractGoldenTest {

    private static final Instant FIXTURE_TIME = Instant.parse("2026-06-09T12:00:01Z");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Path FIXTURE_DIR = Path.of("src/test/resources/fixtures/charts");

    @Test
    void simulationYieldProjectionMatchesGoldenFixture() throws Exception {
        ChartContract chart = ChartBuilders.yieldProjection(
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
        assertMatchesFixture("simulation-yield-projection.json", chart);
    }

    @Test
    void liquidationBandMatchesGoldenFixture() throws Exception {
        ChartContract chart = ChartBuilders.liquidationBand(
                "maker_sky",
                FinancialMath.bd("3800"),
                FinancialMath.bd("3166.67"),
                FinancialMath.bd("2"),
                FinancialMath.bd("4222.22"),
                "chainlink",
                false,
                "ETH spot (Chainlink)",
                FIXTURE_TIME
        );
        assertMatchesFixture("liquidation-price-band.json", chart);
    }

    @Test
    void healthRatioSweepMatchesGoldenFixture() throws Exception {
        ChartContract chart = ChartBuilders.healthRatioSweep(
                "maker_sky",
                FinancialMath.bd("2"),
                FinancialMath.bd("4222.22"),
                FinancialMath.bd("1.50"),
                FinancialMath.bd("3800"),
                "chainlink",
                false,
                FIXTURE_TIME
        );
        assertMatchesFixture("health-ratio-sweep.json", chart);
    }

    @Test
    void treasuryContextMatchesGoldenFixture() throws Exception {
        ChartContract chart = ChartBuilders.treasuryContextChart(
                FinancialMath.bd("3800.00"),
                FinancialMath.bd("171.00"),
                FinancialMath.bd("4.91"),
                "maker_sky",
                FinancialMath.bd("4222.22"),
                FIXTURE_TIME
        );
        assertMatchesFixture("stablecoin-treasury-context.json", chart);
    }

    @Test
    void protocolRatesMatchesGoldenFixture() throws Exception {
        ChartContract chart = ChartBuilders.protocolRatesComparison(
                "USDC",
                List.of(
                        new YieldQuote("aave", FinancialMath.bd("4.2"), com.ethsimulator.protocol.RateConvention.APR_EFFECTIVE, "seed", RiskTier.LOW, FIXTURE_TIME, true),
                        new YieldQuote("compound", FinancialMath.bd("3.8"), com.ethsimulator.protocol.RateConvention.APR_EFFECTIVE, "seed", RiskTier.LOW, FIXTURE_TIME, true),
                        new YieldQuote("maker_dsr", FinancialMath.bd("5.0"), com.ethsimulator.protocol.RateConvention.APR_EFFECTIVE, "seed", RiskTier.MEDIUM, FIXTURE_TIME, true)
                ),
                FIXTURE_TIME
        );
        assertMatchesFixture("protocol-rates-comparison.json", chart);
    }

    @Test
    void ethPriceHistoryMatchesGoldenFixture() throws Exception {
        ChartContract chart = ChartBuilders.ethPriceHistory(
                new EthPriceHistoryService().seedHistory(),
                "seed_history",
                false,
                FIXTURE_TIME
        );
        assertMatchesFixture("eth-price-history.json", chart);
    }

    private static void assertMatchesFixture(String fixtureName, ChartContract chart) throws Exception {
        JsonNode expected = MAPPER.readTree(readFixture(fixtureName));
        JsonNode actual = MAPPER.readTree(MAPPER.writeValueAsString(chart));
        assertEquals(expected, actual, () -> "Fixture drift for " + fixtureName);
    }

    private static String readFixture(String fixtureName) throws Exception {
        return Files.readString(FIXTURE_DIR.resolve(fixtureName), StandardCharsets.UTF_8);
    }

    static Stream<String> goldenFixtureNames() {
        return Stream.of(
                "simulation-yield-projection.json",
                "liquidation-price-band.json",
                "health-ratio-sweep.json",
                "stablecoin-treasury-context.json",
                "protocol-rates-comparison.json",
                "eth-price-history.json"
        );
    }
}