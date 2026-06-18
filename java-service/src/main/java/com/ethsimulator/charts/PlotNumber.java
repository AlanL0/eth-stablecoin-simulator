package com.ethsimulator.charts;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonSerialize;

import java.math.BigDecimal;

/** Presentation-only coordinate serialized as a bare JSON number. */
@JsonSerialize(using = PlotNumberSerializer.class)
@Schema(implementation = BigDecimal.class)
record PlotNumber(BigDecimal value) {

    static PlotNumber of(BigDecimal value) {
        return new PlotNumber(value);
    }
}