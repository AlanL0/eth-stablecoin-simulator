package com.ethsimulator.charts;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

final class PlotNumberSerializer extends StdSerializer<PlotNumber> {

    PlotNumberSerializer() {
        super(PlotNumber.class);
    }

    @Override
    public void serialize(PlotNumber value, JsonGenerator gen, SerializationContext ctxt) {
        if (value == null || value.value() == null) {
            gen.writeNull();
            return;
        }
        gen.writeNumber(value.value());
    }
}