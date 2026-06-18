package com.ethsimulator.charts;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.math.BigDecimal;

/**
 * Serializes presentation-only plot coordinates as JSON numbers (not decimal strings).
 */
final class PlotValueSerializer extends StdSerializer<BigDecimal> {

    PlotValueSerializer() {
        super(BigDecimal.class);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext ctxt) {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeNumber(value);
    }
}