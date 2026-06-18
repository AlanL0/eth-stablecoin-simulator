package com.ethsimulator.qa;

import com.ethsimulator.charts.ChartBuilders;
import com.ethsimulator.charts.ChartContract;

import com.ethsimulator.protocol.AnnualizedRate;
import com.ethsimulator.protocol.RateConvention;
import com.ethsimulator.protocol.RateMath;
import com.ethsimulator.simulation.SimulationEngine;
import com.ethsimulator.simulation.SimulationLimits;
import com.ethsimulator.util.EvmAddressValidator;
import com.ethsimulator.util.FinancialMath;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Independent QA coverage for ETH-T25 precision, convention, and determinism requirements.
 */
class PrecisionQaTest {

    private static final Instant FIXTURE_TIME = Instant.parse("2026-06-09T12:00:01Z");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest(name = "add {0} + {1} = {2}")
    @CsvSource({
            "0, 0, 0",
            "1, -1, 0",
            "-2.5, -1.5, -4",
            "0.1, 0.2, 0.3",
            "999999999999999999, 1, 1000000000000000000"
    })
    void parameterizedAddSubtractSignsAndZero(String left, String right, String expected) {
        BigDecimal result = FinancialMath.add(FinancialMath.bd(left), FinancialMath.bd(right));
        assertEquals(0, FinancialMath.bd(expected).compareTo(result));
    }

    @ParameterizedTest(name = "divide {0}/{1} scale={2} => {3}")
    @CsvSource({
            "1, 3, 2, 0.33",
            "10, 4, 2, 2.50",
            "1, 7, 8, 0.14285714",
            "100, 3, 4, 33.3333"
    })
    void parameterizedExactAndRepeatingDivision(String dividend, String divisor, int scale, String expected) {
        assertEquals(
                FinancialMath.bd(expected),
                FinancialMath.divide(FinancialMath.bd(dividend), FinancialMath.bd(divisor), scale)
        );
    }

    @ParameterizedTest
    @MethodSource("scaleTransitionCases")
    void parameterizedScaleTransitions(String input, String usd, String eth, String rate) {
        BigDecimal value = FinancialMath.bd(input);
        assertEquals(FinancialMath.bd(usd), FinancialMath.scaleUsd(value));
        assertEquals(FinancialMath.bd(eth), FinancialMath.scaleEth(value));
        assertEquals(FinancialMath.bd(rate), FinancialMath.scaleRate(value));
    }

    static Stream<Arguments> scaleTransitionCases() {
        return Stream.of(
                Arguments.of("1234.56789", "1234.57", "1234.5679", "1234.56789000"),
                Arguments.of("0.00001", "0.00", "0.0000", "0.00001000"),
                Arguments.of("-1.23456", "-1.23", "-1.2346", "-1.23456000")
        );
    }

    @ParameterizedTest
    @CsvSource({
            "500, 0.05000000, 5.00",
            "1, 0.00010000, 0.01",
            "10000, 1.00000000, 100.00"
    })
    void parameterizedBasisPoints(int bps, String decimalRate, String humanPercent) {
        assertEquals(FinancialMath.bd(decimalRate), RateMath.bpsToDecimalRate(bps));
        assertEquals(FinancialMath.bd(humanPercent), RateMath.bpsToHumanPercent(bps));
    }

    @ParameterizedTest
    @CsvSource({
            "1.5, 1500000000000000000, 1.500000000000000000",
            "0, 0, 0.000000000000000000",
            "-0.25, -250000000000000000, -0.250000000000000000"
    })
    void parameterizedWadRayWeiConversions(String decimal, String wad, String fromWad) {
        BigDecimal value = FinancialMath.bd(decimal);
        assertEquals(FinancialMath.bd(wad), FinancialMath.wadFromDecimal(value));
        assertEquals(FinancialMath.bd(fromWad), FinancialMath.decimalFromWad(FinancialMath.bd(wad)));
        assertEquals(FinancialMath.bd(wad), FinancialMath.scaleWei(FinancialMath.wadFromDecimal(value)));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 12, 0.12000000",
            "50, 4, 2.00000000"
    })
    void parameterizedSimpleApr(String ratePerPeriodPercent, int periodsPerYear, String expectedApr) {
        BigDecimal ratePerPeriod = FinancialMath.humanPercentToRate(FinancialMath.bd(ratePerPeriodPercent));
        assertEquals(FinancialMath.bd(expectedApr), FinancialMath.simpleApr(ratePerPeriod, periodsPerYear));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 12, 0.12682503",
            "5, 1, 0.05000000"
    })
    void parameterizedEffectiveApy(String ratePerPeriodPercent, int periodsPerYear, String expectedApy) {
        BigDecimal ratePerPeriod = FinancialMath.humanPercentToRate(FinancialMath.bd(ratePerPeriodPercent));
        assertEquals(FinancialMath.bd(expectedApy), FinancialMath.effectiveApy(ratePerPeriod, periodsPerYear));
    }

    @Test
    void zeroDivisorRejected() {
        assertThrows(ArithmeticException.class,
                () -> FinancialMath.divide(FinancialMath.bd("1"), BigDecimal.ZERO, 2));
    }

    @Test
    void negativeChainlinkAnswerRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> RateMath.chainlinkAnswerToUsd(BigInteger.valueOf(-1), 8));
    }

    @Test
    void unsupportedAnnualizationElapsedPeriodsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> FinancialMath.trailingRealizedApr(FinancialMath.bd("0.01"), 0, 12));
    }

    @Test
    void malformedDecimalJsonRejected() throws Exception {
        JsonNode objectNode = MAPPER.readTree("{\"value\":true}");
        assertThrows(IllegalArgumentException.class, () -> FinancialMath.parseJsonNumber(objectNode));
        assertThrows(IllegalArgumentException.class, () -> FinancialMath.parseJsonNumber(null));
        JsonNode arrayNode = MAPPER.readTree("[1,2]");
        assertThrows(IllegalArgumentException.class, () -> FinancialMath.parseJsonNumber(arrayNode));
    }

    @Test
    void financialMathNullGuardsAndAggregates() {
        assertThrows(IllegalArgumentException.class, () -> FinancialMath.requireNonNull(null, "amount"));
        assertThrows(IllegalArgumentException.class, () -> FinancialMath.add(null, FinancialMath.bd("1")));
        assertThrows(IllegalArgumentException.class, () -> FinancialMath.subtract(null, FinancialMath.bd("1")));
        assertThrows(IllegalArgumentException.class, () -> FinancialMath.multiply(FinancialMath.bd("1"), null));
        assertEquals(FinancialMath.bd("3"), FinancialMath.max(FinancialMath.bd("3"), FinancialMath.bd("1")));
        assertEquals(FinancialMath.bd("1"), FinancialMath.min(FinancialMath.bd("3"), FinancialMath.bd("1")));
        assertEquals(42, FinancialMath.toIntRounded(FinancialMath.bd("41.6")));
        assertEquals(FinancialMath.bd("2"), FinancialMath.bd(2));
        assertEquals(FinancialMath.bd("99"), FinancialMath.bd(99L));
        assertEquals(FinancialMath.bd("1.500000000000000000000000000"),
                FinancialMath.decimalFromRay(FinancialMath.rayFromDecimal(FinancialMath.bd("1.5"))));
    }

    @Test
    void annualizedRateScalesSpotUsdAtUsdPrecision() {
        AnnualizedRate spot = AnnualizedRate.of(FinancialMath.bd("3500.567"), RateConvention.SPOT_USD, "oracle");
        assertEquals(FinancialMath.bd("3500.57"), spot.value());
    }

    @Test
    void evmAddressValidatorRejectsInvalidAddresses() {
        assertEquals("0xd8da6bf26964af9d7eed9e03e53415d37aa96045",
                EvmAddressValidator.requireValid("0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"));
        assertThrows(com.ethsimulator.api.error.ApiException.class,
                () -> EvmAddressValidator.requireValid("not-an-address"));
        assertThrows(com.ethsimulator.api.error.ApiException.class,
                () -> EvmAddressValidator.requireValid(null));
    }

    @Test
    void plotCoordinateSerializersEmitJsonNumbers() throws Exception {
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

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(chart));
        assertTrue(json.path("yAxis").path("domain").get(0).isNumber());
        assertTrue(json.path("series").get(0).path("data").get(0).path("plotValue").isNumber());
        assertEquals("1.2", json.path("series").get(0).path("data").get(2).path("displayValue").asString());
    }

    @Test
    void excessiveSimulationCompoundingRejected() {
        assertThrows(com.ethsimulator.api.error.ApiException.class,
                () -> SimulationLimits.validateCompounding(51, 12));
        assertThrows(com.ethsimulator.api.error.ApiException.class,
                () -> SimulationLimits.validateCompounding(10, 400));
        assertThrows(com.ethsimulator.api.error.ApiException.class,
                () -> SimulationLimits.validateCompounding(100, 200));
    }

    @Test
    void nullAnnualizedRateConventionRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AnnualizedRate(FinancialMath.bd("5"), null, "fixture"));
    }

    @Test
    void nullRequiredChartProvenanceRejected() {
        ChartContract chart = new ChartContract(
                ChartContract.SCHEMA_VERSION,
                "missing_provenance",
                "Missing provenance",
                null,
                new ChartContract.ChartAxis("linear", "X", null, null, null, null),
                new ChartContract.ChartAxis("linear", "Y", null, null, null, null),
                List.of(new ChartContract.ChartSeries(
                        "s1",
                        "Series",
                        "usd",
                        new ChartContract.ChartSeriesStyle("line", "primary", null, null),
                        List.of(new ChartContract.DataPoint(
                                "1",
                                FinancialMath.bd("1.00"),
                                "1.00",
                                null,
                                null
                        ))
                )),
                List.of(),
                Map.of(),
                List.of(),
                null
        );

        assertFalse(VALIDATOR.validate(chart).isEmpty());
    }

    @Test
    void canonicalSimulationBytesStableAcrossLocaleAndTimezone() throws Exception {
        Locale defaultLocale = Locale.getDefault();
        TimeZone defaultZone = TimeZone.getDefault();
        try {
            for (Locale locale : List.of(Locale.US, Locale.FRANCE, Locale.JAPAN)) {
                for (String zoneId : List.of("UTC", "America/Los_Angeles", "Asia/Tokyo")) {
                    Locale.setDefault(locale);
                    TimeZone.setDefault(TimeZone.getTimeZone(zoneId));

                    byte[] payload = normalizedSimulationJson();
                    byte[] expected = normalizedSimulationJson();
                    assertArrayEquals(expected, payload, "locale=" + locale + " zone=" + zoneId);
                }
            }
        } finally {
            Locale.setDefault(defaultLocale);
            TimeZone.setDefault(defaultZone);
        }
    }

    @Test
    void chartDisplayValuesRemainExactDecimalStrings() throws Exception {
        ChartContract chart = ChartBuilders.ethPriceHistory(
                List.of(new ChartBuilders.EthHistoryPoint(
                        "2026-06-01T00:00:00Z",
                        FinancialMath.bd("3521.445678901234567890"))),
                "seed_history",
                false,
                FIXTURE_TIME
        );

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(chart));
        JsonNode displayValue = json.path("series").get(0).path("data").get(0).path("displayValue");
        assertTrue(displayValue.isString());
        assertEquals("3521.445678901234567890", displayValue.asString());
        assertFalse(displayValue.asString().contains("e"));
        assertFalse(displayValue.asString().contains("E"));
    }

    @ParameterizedTest
    @CsvSource({
            "3925.5, 3925.5",
            "1.23e-4, 0.000123",
            "0.000123, 0.000123"
    })
    void jsonNumbersParsedFromTextNeverThroughFloatingPoint(String raw, String expected) throws Exception {
        JsonNode node = MAPPER.readTree(raw);
        assertEquals(FinancialMath.bd(expected), FinancialMath.parseJsonNumber(node));
    }

    private static byte[] normalizedSimulationJson() throws Exception {
        SimulationEngine.Result result = SimulationEngine.compute(
                FinancialMath.bd("2"),
                FinancialMath.bd("3800"),
                FinancialMath.bd("1.80"),
                FinancialMath.bd("1.50"),
                FinancialMath.bd("5.00"),
                FinancialMath.bd("5.00"),
                1,
                12
        );

        String json = MAPPER.writeValueAsString(Map.of(
                "collateralValueUsd", result.collateralValueUsd().toPlainString(),
                "stablecoinDebtUsd", result.stablecoinDebtUsd().toPlainString(),
                "liquidationPriceUsd", result.liquidationPriceUsd().toPlainString(),
                "annualStabilityFeeUsd", result.annualStabilityFeeUsd().toPlainString(),
                "projectedGrossYieldUsd", result.projectedGrossYieldUsd().toPlainString(),
                "projectedNetYieldUsd", result.projectedNetYieldUsd().toPlainString(),
                "healthRatio", result.healthRatio().toPlainString(),
                "riskTier", result.riskTier().name()
        ));
        return json.getBytes(StandardCharsets.UTF_8);
    }
}