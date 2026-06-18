package com.ethsimulator.charts;

import com.ethsimulator.util.FinancialMath;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringWriter;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartSerializationTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void plotValueSerializerWritesNullForMissingCoordinate() throws Exception {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = MAPPER.createGenerator(writer);
        new PlotValueSerializer().serialize(null, generator, (SerializationContext) null);
        generator.flush();
        assertEquals("null", writer.toString());
    }

    @Test
    void plotNumberSerializerWritesNullForMissingDomainValue() throws Exception {
        var json = MAPPER.readTree(MAPPER.writeValueAsString(PlotNumber.of(null)));
        assertTrue(json.isNull());

        StringWriter writer = new StringWriter();
        JsonGenerator generator = MAPPER.createGenerator(writer);
        new PlotNumberSerializer().serialize(null, generator, (SerializationContext) null);
        generator.flush();
        assertEquals("null", writer.toString());
    }

    @Test
    void seriesStyleFillOpacitySerializesAsNumber() throws Exception {
        ChartContract.ChartSeriesStyle style = new ChartContract.ChartSeriesStyle(
                "area",
                "positive",
                null,
                FinancialMath.bd("0.2")
        );
        var json = MAPPER.readTree(MAPPER.writeValueAsString(style));
        assertTrue(json.path("fillOpacity").isNumber());
    }
}