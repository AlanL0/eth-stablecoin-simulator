package com.ethsimulator.charts;

import com.ethsimulator.util.FinancialMath;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartContractTest {

    private static final Instant FIXTURE_TIME = Instant.parse("2026-06-09T12:00:01Z");
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void schemaVersionIsTwoPointZero() {
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

        assertEquals(ChartContract.SCHEMA_VERSION, chart.schemaVersion());
    }

    @Test
    void preservesExactDisplayValueForLongDecimals() throws Exception {
        ChartContract chart = ChartBuilders.ethPriceHistory(
                List.of(new ChartBuilders.EthHistoryPoint(
                        "2026-06-01T00:00:00Z",
                        FinancialMath.bd("3521.445678901234567890"))),
                "seed_history",
                false,
                FIXTURE_TIME
        );

        ChartContract.DataPoint point = chart.series().get(0).data().get(0);
        assertEquals("3521.445678901234567890", point.displayValue());
        assertEquals(FinancialMath.bd("3521.45"), point.plotValue());

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(chart));
        assertEquals("3521.445678901234567890",
                json.path("series").get(0).path("data").get(0).path("displayValue").asString());
        assertTrue(json.path("series").get(0).path("data").get(0).path("plotValue").isNumber());
    }

    @Test
    void emptySeriesFailsValidation() {
        ChartContract empty = new ChartContract(
                ChartContract.SCHEMA_VERSION,
                "empty_chart",
                "Empty",
                null,
                new ChartContract.ChartAxis("linear", "X", null, null, null, null),
                new ChartContract.ChartAxis("linear", "Y", null, null, null, null),
                List.of(new ChartContract.ChartSeries("s1", "Series", "usd",
                        new ChartContract.ChartSeriesStyle("line", "primary", null, null),
                        List.of())),
                List.of(),
                Map.of("chartType", "line"),
                List.of("No data available"),
                new ChartContract.ChartProvenance(
                        "test",
                        FIXTURE_TIME.toString(),
                        null,
                        List.of(new ChartContract.ChartSource("field", "seed", FIXTURE_TIME.toString(), false))
                )
        );

        Set<ConstraintViolation<ChartContract>> violations = VALIDATOR.validate(empty);
        assertFalse(violations.isEmpty());
    }

    @Test
    void staleChartIncludesWarnings() {
        ChartContract chart = ChartBuilders.liquidationBand(
                "maker_sky",
                FinancialMath.bd("3800"),
                FinancialMath.bd("3166.67"),
                FinancialMath.bd("2"),
                FinancialMath.bd("4222.22"),
                "chainlink",
                true,
                "ETH spot (Chainlink)",
                FIXTURE_TIME
        );

        assertNotNull(chart.warnings());
        assertFalse(chart.warnings().isEmpty());
        assertTrue(chart.provenance().sources().get(0).stale());
    }

    @Test
    void warningHeavyChartSerializes() throws Exception {
        ChartContract chart = ChartBuilders.protocolRatesComparison(
                "UNKNOWN",
                List.of(),
                FIXTURE_TIME
        );

        assertTrue(chart.warnings().isEmpty());
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(chart));
        assertEquals("2.0", json.path("schemaVersion").asString());
        assertEquals("protocol_rates_comparison", json.path("chartId").asString());
    }
}